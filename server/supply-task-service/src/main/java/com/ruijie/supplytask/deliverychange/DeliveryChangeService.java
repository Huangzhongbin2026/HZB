package com.ruijie.supplytask.deliverychange;

import com.ruijie.supplytask.common.BusinessException;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

@Service
public class DeliveryChangeService {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final Map<String, Long> signatureCache = new HashMap<>();
    private final List<Map<String, Object>> records = new CopyOnWriteArrayList<>();
    private final Map<String, String> virtualReply = Map.of(
            "RG-SF2920U", "该型号属于虚拟产品，建议按系统自动排产处理。",
            "RG-VIRTUAL-100", "虚拟产品无需实体排产，请按授权流程处理。"
    );

    public Map<String, String> generateSignature(Map<String, String> body) {
        String sysId = value(body, "sys_id");
        String secret = value(body, "access_key_secret");
        if (sysId.isBlank() || secret.isBlank()) {
            throw new BusinessException(2201, "sys_id 或 access_key_secret 不能为空");
        }
        long timestamp = System.currentTimeMillis();
        String digest = md5(sysId + timestamp + secret).toUpperCase();
        String signature = sysId + "|" + timestamp + "|" + digest;
        signatureCache.put(signature, timestamp);
        return Map.of("signature", signature);
    }

    public Map<String, Object> analyzeStep1(Map<String, Object> body, String sysId, String signServerAuth) {
        verifyAuthHeaders(sysId, signServerAuth);
        String contractNo = value(body, "contractNo").replaceAll("\\s+", "");
        String taskType = value(body, "taskType");
        if (taskType.isBlank()) {
            taskType = "客期提前";
        }
        if (contractNo.isBlank()) {
            throw new BusinessException(2202, "合同编号不能为空");
        }

        Map<String, String> aiPayload = body.get("aiPayload") instanceof Map<?, ?>
                ? ((Map<?, ?>) body.get("aiPayload")).entrySet().stream().collect(Collectors.toMap(
                x -> String.valueOf(x.getKey()), x -> String.valueOf(x.getValue()), (a, b) -> b))
                : Map.of();

        List<Map<String, Object>> orderData = mockOrderData(contractNo);
        if (orderData.isEmpty()) {
            throw new BusinessException(2203, "哎呀，您输入的合同编号好像和我们系统里的小伙伴走散啦~ 麻烦您重新输入正确的合同编号，让它归队继续前进吧！");
        }

        List<Map<String, Object>> productArray = new ArrayList<>();
        for (Map<String, Object> row : orderData) {
            Map<String, Object> copy = new LinkedHashMap<>(row);
            String marketCode = asText(copy.get("市场代码名称"));
            boolean selectable = !marketCode.isBlank() && !"null".equalsIgnoreCase(marketCode);
            copy.put("selected", selectable);
            copy.put("可勾选", selectable);
            copy.put("变更数量", toInt(copy.get("未发货数量")));
            copy.put("客期提前至", "");
            copy.put("客期延后至", "");
            productArray.add(copy);
        }

        String today = LocalDate.now().format(DATE);
        List<String> expectDates = orderData.stream().map(x -> asText(x.get("客户期望交期"))).filter(x -> !x.isBlank()).toList();
        String maxExpectDate = expectDates.stream().max(String::compareTo).orElse("");
        String minExpectDate = expectDates.stream().min(String::compareTo).orElse("");
        boolean allExpectAfterToday = expectDates.stream().allMatch(x -> x.compareTo(today) > 0);

        String target = aiPayload.getOrDefault("targetDeliveryDate", "");
        if (!target.isBlank()) {
            for (Map<String, Object> row : productArray) {
                if (!Boolean.TRUE.equals(row.get("可勾选"))) {
                    continue;
                }
                String expect = asText(row.get("客户期望交期"));
                if ("客期提前".equals(taskType) && expect.compareTo(target) > 0 && target.compareTo(today) >= 0) {
                    row.put("客期提前至", target);
                }
                if ("客期延后".equals(taskType) && expect.compareTo(target) < 0 && target.compareTo(today) > 0) {
                    row.put("客期延后至", target);
                }
            }
        }

        Map<String, Object> first = orderData.get(0);
        Map<String, Object> extracted = new LinkedHashMap<>();
        extracted.put("crmNumber", asText(first.get("CRM项目编码")));
        extracted.put("projectName", asText(first.get("项目名称")));
        extracted.put("salesDept", asText(first.get("部门名称")));
        extracted.put("region", asText(first.get("区域")));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("contractNo", contractNo);
        result.put("taskType", taskType);
        result.put("orderData", orderData);
        result.put("productArray", productArray);
        result.put("allExpectAfterToday", allExpectAfterToday);
        result.put("maxExpectDate", maxExpectDate);
        result.put("minExpectDate", minExpectDate);
        result.put("extracted", extracted);
        result.put("changeReason", aiPayload.getOrDefault("changeReason", ""));
        result.put("targetDeliveryDate", target);
        result.put("allowPartialShipmentIfIncomplete", aiPayload.getOrDefault("allowPartialShipmentIfIncomplete", ""));
        return result;
    }

    public boolean hasDuplicate(String contractNo, String taskType) {
        String normalized = contractNo == null ? "" : contractNo.replaceAll("\\s+", "");
        return records.stream()
                .filter(x -> normalized.equals(asText(x.get("合同编号"))))
                .anyMatch(x -> asText(x.get("任务类型")).equals(taskType));
    }

    public Map<String, Object> queryPlan(String crmNumber, String sysId, String signServerAuth) {
        verifyAuthHeaders(sysId, signServerAuth);
        if (crmNumber == null || crmNumber.isBlank()) {
            return Map.of("盘点记录", List.of(), "项目盘点详情url", "");
        }
        List<Map<String, Object>> list = List.of(
                mapOf(
                        "CRM编号", crmNumber,
                        "产品型号", "RG-IF2920U",
                        "产品名称", "维保服务",
                        "数量", 3,
                        "客期", LocalDate.now().plusDays(1).format(DATE),
                        "是否备货", "是",
                        "勾选或取消备货时间", LocalDate.now().minusDays(2).format(DATE),
                        "一级部门", "平台管理部",
                        "三级部门", "广东区域",
                        "机会所有人", "赵某某",
                        "业务机会名称", "某某项目",
                        "客户名称", "某某客户",
                        "是否退市报备", "否",
                        "产品流速", ""
                )
        );
        return Map.of("盘点记录", list, "项目盘点详情url", "https://marketplan.com.cn/userid=" + crmNumber);
    }

    public Map<String, Object> submit(Map<String, Object> body, String sysId, String signServerAuth) {
        verifyAuthHeaders(sysId, signServerAuth);
        String contractNo = value(body, "contractNo").replaceAll("\\s+", "");
        String requester = value(body, "requesterName");
        String taskType = value(body, "taskType");
        String repeatFlag = value(body, "repeatFlag");
        String changeReason = value(body, "changeReason");
        String delayReason = value(body, "delayReason");
        String delayProof = value(body, "delayProof");
        String allowPartial = value(body, "allowPartialShipment");
        String crmNumber = value(body, "crmNumber");
        String projectPlanUrl = value(body, "projectPlanUrl");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> productArray = body.get("productArray") instanceof List<?> list
                ? list.stream().filter(Map.class::isInstance).map(x -> (Map<String, Object>) x).collect(Collectors.toList())
                : List.of();
        if (productArray.isEmpty()) {
            throw new BusinessException(2204, "提交失败，未选择有效订单行");
        }

        String approvalNo = "KQ" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + (int) (Math.random() * 10);
        String now = LocalDateTime.now().format(DATETIME);

        Map<String, String> regionCoordinator = resolveRegionCoordinator(value(body, "salesDept"), value(body, "region"), value(body, "projectName"));
        String agentRegion = isOnLeave(regionCoordinator.get("name")) ? regionCoordinator.getOrDefault("agentName", "") : "";

        for (Map<String, Object> item : productArray) {
            String marketCode = asText(item.get("市场代码名称"));
            String planner = asText(item.get("供应统筹"));
            String agentPlanner = matchAgentPlanner(marketCode, planner);

            String autoReply = virtualReply.getOrDefault(marketCode, "");
            String status = autoReply.isBlank() ? "未评估" : "已评估";

            Map<String, Object> record = new LinkedHashMap<>();
            record.put("审批编号", approvalNo);
            record.put("任务创建时间", now);
            record.put("合同编号", contractNo);
            record.put("CRM编号", crmNumber);
            record.put("任务类型", taskType);
            record.put("是否重复提交", repeatFlag.isBlank() ? "否" : repeatFlag);
            record.put("提问人", requester);
            record.put("市场代码名称", marketCode);
            record.put("物料描述", asText(item.get("物料描述")));
            record.put("未发货数量", toInt(item.get("未发货数量")));
            record.put("客户期望日期", asText(item.get("客户期望交期")));
            record.put("约定交期-ODC", asText(item.get("约定交期-ODC")));
            record.put("订单金额", item.getOrDefault("订单金额", 0));
            record.put("结算金额(人民币)", item.getOrDefault("结算金额(人民币)", 0));
            record.put("变更数量", toInt(item.get("变更数量")));
            record.put("客期提前至", asText(item.get("客期提前至")));
            record.put("客期延后至", asText(item.get("客期延后至")));
            record.put("下单日期", asText(item.get("下单日期")));
            record.put("产品统筹", planner);
            record.put("代理产品统筹", agentPlanner);
            record.put("订单编号", asText(item.get("订单编号")));
            record.put("订单行号", asText(item.get("订单行")));
            record.put("客户类型", asText(item.get("客户类型")));
            record.put("齐套日期", asText(item.get("齐套日期")));
            record.put("若不齐套是否同意分批发货", allowPartial);
            record.put("商务助理", asText(item.get("商务助理名称")));
            record.put("商务助理飞书ID", asText(item.get("商务助理员工ID")));
            record.put("项目盘点详情url", projectPlanUrl);
            record.put("提前原因", "客期提前".equals(taskType) ? changeReason : "");
            record.put("延后原因", "客期延后".equals(taskType) ? delayReason : "");
            record.put("延后证明", "客期延后".equals(taskType) ? delayProof : "");
            record.put("销售部门", value(body, "salesDept"));
            record.put("项目名称", value(body, "projectName"));
            record.put("区域", value(body, "region"));
            record.put("区域统筹", regionCoordinator.getOrDefault("name", ""));
            record.put("区域统筹飞书ID", regionCoordinator.getOrDefault("feishuId", ""));
            record.put("代理区域统筹", agentRegion);
            record.put("统筹评估回复", autoReply);
            record.put("任务评估状态", status);
            record.put("用户操作", "任务催办");
            record.put("区域统筹操作", "任务催办");
            record.put("任务完成时间", autoReply.isBlank() ? "" : now);
            records.add(record);
        }

        return Map.of("approvalNo", approvalNo, "count", productArray.size());
    }

    public Map<String, Object> queryManagement(Integer pageNo, Integer pageSize,
                                               String approvalNo, String contractNo, String taskType, String createdAt) {
        List<Map<String, Object>> filtered = records.stream()
                .filter(x -> contains(x.get("审批编号"), approvalNo))
                .filter(x -> contains(x.get("合同编号"), contractNo))
                .filter(x -> contains(x.get("任务类型"), taskType))
                .filter(x -> contains(x.get("任务创建时间"), createdAt))
                .sorted(Comparator.comparing(x -> asText(x.get("任务创建时间")), Comparator.reverseOrder()))
                .toList();

        int no = pageNo == null || pageNo < 1 ? 1 : pageNo;
        int size = pageSize == null || pageSize < 1 ? 10 : pageSize;
        int from = (no - 1) * size;
        List<Map<String, Object>> page = from >= filtered.size() ? List.of() : filtered.subList(from, Math.min(from + size, filtered.size()));
        return Map.of("list", page, "total", filtered.size());
    }

    public Boolean saveEvaluation(String approvalNo, String marketCode, String reply) {
        List<Map<String, Object>> rows = records.stream()
                .filter(x -> approvalNo.equals(asText(x.get("审批编号"))))
                .filter(x -> marketCode == null || marketCode.isBlank() || marketCode.equals(asText(x.get("市场代码名称"))))
                .collect(Collectors.toList());
        if (rows.isEmpty()) {
            throw new BusinessException(2205, "任务不存在");
        }
        String now = LocalDateTime.now().format(DATETIME);
        for (Map<String, Object> row : rows) {
            String text = reply == null ? "" : reply;
            row.put("统筹评估回复", text);
            row.put("任务评估状态", text.isBlank() ? "未评估" : "已评估");
            row.put("任务完成时间", text.isBlank() ? "" : now);
        }
        return Boolean.TRUE;
    }

    public Boolean urgeUser(String approvalNo) {
        return records.stream().anyMatch(x -> approvalNo.equals(asText(x.get("审批编号"))));
    }

    public Boolean urgeRegion(String approvalNo) {
        return records.stream().anyMatch(x -> approvalNo.equals(asText(x.get("审批编号"))));
    }

    private List<Map<String, Object>> mockOrderData(String contractNo) {
        if (!contractNo.matches("[A-Za-z0-9-]{6,}")) {
            return List.of();
        }
        if ("100226031700302".equals(contractNo)) {
            return List.of(
                mapOf(
                    "合同编号", contractNo,
                    "订单编号", "11101117663",
                    "订单行", "1.1",
                    "供应统筹", "黄某某",
                    "商务助理名称", "黄某某",
                    "商务助理员工ID", "ou_9e45b18b0814fbef6b59d9f3f259bdb6",
                    "约定交期-ODC", LocalDate.now().plusDays(9).format(DATE),
                    "CRM项目编码", "20240613000157",
                    "项目名称", "广州校园网络项目",
                    "市场代码名称", "RG-PA1600I",
                    "未发货数量", 6,
                    "部门名称", "平台管理部",
                    "区域", "广东",
                    "齐套日期", LocalDate.now().plusDays(4).format(DATE),
                    "客户类型", "代理商",
                    "客户期望交期", LocalDate.now().plusDays(6).format(DATE),
                    "下单日期", LocalDate.now().minusDays(1).format(DATE),
                    "结算金额(人民币)", 1800,
                    "订单金额", 5200,
                    "物料描述", "电源模块",
                    "订单类型", "非特价下单",
                    "业务员姓名", "黄忠彬"
                ),
                mapOf(
                    "合同编号", contractNo,
                    "订单编号", "11101117663",
                    "订单行", "2.1",
                    "供应统筹", "王某某",
                    "商务助理名称", "黄某某",
                    "商务助理员工ID", "ou_9e45b18b0814fbef6b59d9f3f259bdb6",
                    "约定交期-ODC", LocalDate.now().plusDays(11).format(DATE),
                    "CRM项目编码", "20240613000157",
                    "项目名称", "广州校园网络项目",
                    "市场代码名称", "RG-PA1600I-F",
                    "未发货数量", 2,
                    "部门名称", "平台管理部",
                    "区域", "广东",
                    "齐套日期", LocalDate.now().plusDays(5).format(DATE),
                    "客户类型", "代理商",
                    "客户期望交期", LocalDate.now().plusDays(7).format(DATE),
                    "下单日期", LocalDate.now().minusDays(1).format(DATE),
                    "结算金额(人民币)", 700,
                    "订单金额", 2200,
                    "物料描述", "电源模块-F",
                    "订单类型", "非特价下单",
                    "业务员姓名", "黄忠彬"
                )
            );
        }
        return List.of(
                mapOf(
                        "合同编号", contractNo,
                        "订单编号", "11101117663",
                        "订单行", "1.1",
                        "供应统筹", "黄某某",
                        "商务助理名称", "黄某某",
                        "商务助理员工ID", "ou_abda0d6bc814be1fcd7318a85bf3c1e7",
                        "约定交期-ODC", "2026-03-24",
                        "CRM项目编码", "20251010000225",
                        "项目名称", "某某项目",
                        "市场代码名称", "RG-SF2920U",
                        "未发货数量", 3,
                        "部门名称", "平台管理部",
                        "区域", "广东",
                        "齐套日期", "2026-04-11",
                        "客户类型", "代理商",
                        "客户期望交期", "2026-03-22",
                        "下单日期", "2026-03-20",
                        "结算金额(人民币)", 0,
                        "订单金额", 4000,
                        "物料描述", "基础质保服务",
                        "订单类型", "非特价下单",
                        "业务员姓名", "赵某某"
                ),
                mapOf(
                        "合同编号", contractNo,
                        "订单编号", "11101117663",
                        "订单行", "4.1",
                        "供应统筹", "王某某",
                        "商务助理名称", "黄某某",
                        "商务助理员工ID", "ou_abda0d6bc814be1fcd7318a85bf3c1e7",
                        "约定交期-ODC", "2026-03-26",
                        "CRM项目编码", "20251010000225",
                        "项目名称", "某某项目",
                        "市场代码名称", "RG-MF2920E",
                        "未发货数量", 5,
                        "部门名称", "平台管理部",
                        "区域", "广东",
                        "齐套日期", "2026-04-11",
                        "客户类型", "代理商",
                        "客户期望交期", "2026-03-23",
                        "下单日期", "2026-03-20",
                        "结算金额(人民币)", 800,
                        "订单金额", 4000,
                        "物料描述", "出厂整机",
                        "订单类型", "非特价下单",
                        "业务员姓名", "赵某某"
                ),
                mapOf(
                        "合同编号", contractNo,
                        "订单编号", "11101117663",
                        "订单行", "5.1",
                        "供应统筹", "",
                        "商务助理名称", "黄某某",
                        "商务助理员工ID", "ou_abda0d6bc814be1fcd7318a85bf3c1e7",
                        "约定交期-ODC", "2026-03-26",
                        "CRM项目编码", "20251010000225",
                        "项目名称", "某某项目",
                        "市场代码名称", "",
                        "未发货数量", 1,
                        "部门名称", "平台管理部",
                        "区域", "广东",
                        "齐套日期", "2026-04-11",
                        "客户类型", "代理商",
                        "客户期望交期", "2026-03-23",
                        "下单日期", "2026-03-20",
                        "结算金额(人民币)", 100,
                        "订单金额", 100,
                        "物料描述", "N/A",
                        "订单类型", "非特价下单",
                        "业务员姓名", "赵某某"
                )
        );
    }

    private Map<String, String> resolveRegionCoordinator(String salesDept, String region, String projectName) {
        String dept = normalize(salesDept);
        String reg = normalize(region);
        String project = normalize(projectName);

        List<Map<String, String>> rules = List.of(
                rule("ebg", "深圳", "", "张某某", "ou_region_zhang", "张代理"),
                rule("cbg", "", "教育", "李某某", "ou_region_li", "李代理"),
                rule("tbu", "", "政企", "王某某", "ou_region_wang", "王代理"),
                rule("平台管理部", "广东", "", "黄某某", "ou_region_huang", "赵代理")
        );

        for (Map<String, String> rule : rules) {
            if (!dept.contains(rule.get("dept"))) {
                continue;
            }
            String regionKeyword = normalize(rule.get("region"));
            String projectKeyword = normalize(rule.get("projectKeyword"));
            boolean match = (!regionKeyword.isBlank() && reg.contains(regionKeyword))
                    || (!projectKeyword.isBlank() && project.contains(projectKeyword))
                    || (regionKeyword.isBlank() && projectKeyword.isBlank());
            if (match) {
                return Map.of(
                        "name", rule.get("name"),
                        "feishuId", rule.get("feishuId"),
                        "agentName", rule.get("agentName")
                );
            }
        }
        return Map.of("name", "", "feishuId", "", "agentName", "");
    }

    private static Map<String, String> rule(String dept, String region, String projectKeyword,
                                            String name, String feishuId, String agentName) {
        return Map.of(
                "dept", dept,
                "region", region,
                "projectKeyword", projectKeyword,
                "name", name,
                "feishuId", feishuId,
                "agentName", agentName
        );
    }

    private boolean isOnLeave(String userName) {
        return "黄某某".equals(userName) || "张某某".equals(userName);
    }

    private String matchAgentPlanner(String model, String planner) {
        if (model == null || model.isBlank()) {
            return "";
        }
        if (model.contains("SF")) {
            return "李代理";
        }
        if (planner != null && !planner.isBlank()) {
            return planner + "(代理)";
        }
        return "";
    }

    private void verifyAuthHeaders(String sysId, String signServerAuth) {
        if (sysId == null || sysId.isBlank() || signServerAuth == null || signServerAuth.isBlank()) {
            throw new BusinessException(2206, "请求头缺少 sysId 或 sign-server-auth");
        }
        String[] parts = signServerAuth.split("\\|");
        if (parts.length != 3) {
            throw new BusinessException(2207, "sign-server-auth 格式不合法");
        }
        long ts;
        try {
            ts = Long.parseLong(parts[1]);
        } catch (Exception ex) {
            throw new BusinessException(2207, "sign-server-auth 格式不合法");
        }
        if (System.currentTimeMillis() - ts > 5 * 60 * 1000L) {
            throw new BusinessException(2208, "signature 已过期，请重新获取");
        }
    }

    private boolean contains(Object source, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return true;
        }
        return asText(source).contains(keyword.trim());
    }

    private String value(Map<String, ?> body, String key) {
        if (body == null || body.get(key) == null) {
            return "";
        }
        return String.valueOf(body.get(key));
    }

    private String asText(Object value) {
        if (value == null) {
            return "";
        }
        String text = String.valueOf(value);
        return "null".equalsIgnoreCase(text) ? "" : text;
    }

    private Integer toInt(Object value) {
        if (value instanceof Number n) {
            return n.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (Exception ex) {
            return 0;
        }
    }

    private String normalize(String text) {
        return text == null ? "" : text.toLowerCase().replaceAll("\\s+", "");
    }

    private String md5(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] bytes = digest.digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception ex) {
            throw new BusinessException(2200, "签名生成失败");
        }
    }

    @SafeVarargs
    private final Map<String, Object> mapOf(Object... kv) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i + 1 < kv.length; i += 2) {
            map.put(String.valueOf(kv[i]), kv[i + 1]);
        }
        return map;
    }
}
