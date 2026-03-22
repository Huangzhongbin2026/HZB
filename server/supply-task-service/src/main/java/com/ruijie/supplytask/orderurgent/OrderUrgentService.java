package com.ruijie.supplytask.orderurgent;

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
public class OrderUrgentService {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final List<Map<String, Object>> urgentRecords = new CopyOnWriteArrayList<>();
    private final Map<String, Long> signatureCache = new HashMap<>();

    public Map<String, String> generateSignature(Map<String, String> body) {
        String sysId = value(body, "sys_id");
        String secret = value(body, "access_key_secret");
        if (sysId.isBlank() || secret.isBlank()) {
            throw new BusinessException(2001, "sys_id 或 access_key_secret 不能为空");
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
        if (contractNo.isBlank()) {
            throw new BusinessException(2002, "合同编号不能为空");
        }

        List<Map<String, Object>> orderData = mockOrderData(contractNo);
        if (orderData.isEmpty()) {
            throw new BusinessException(2003, "哎呀，您输入的合同编号好像和我们系统里的小伙伴走散啦~ 麻烦您重新输入正确的合同编号，让它归队继续前进吧！");
        }

        List<String> orderPlaceDateList = collect(orderData, "下单日期");
        List<String> expectDeliveryDateList = collect(orderData, "客户期望交期");
        List<String> agreedDeliveryOdcList = collect(orderData, "约定交期-ODC");
        String orderType = String.valueOf(orderData.get(0).getOrDefault("订单类型", ""));

        String odcEmptyStatus = checkEmptyStatus(agreedDeliveryOdcList);
        String today = LocalDate.now().format(DATE);
        boolean hasTodayOrder = orderPlaceDateList.stream().anyMatch(today::equals);

        List<Map<String, String>> emptySearchResult = new ArrayList<>();
        for (Map<String, Object> item : orderData) {
            String orderDate = String.valueOf(item.getOrDefault("下单日期", ""));
            String odc = String.valueOf(item.getOrDefault("约定交期-ODC", ""));
            if (today.equals(orderDate) && (odc == null || odc.isBlank() || "null".equalsIgnoreCase(odc))) {
                emptySearchResult.add(Map.of(
                        "orderNum", String.valueOf(item.getOrDefault("订单编号", "")),
                        "lineNumber", String.valueOf(item.getOrDefault("订单行", "")),
                        "reason", "待统筹维护交期，预计下单后第二个工作日可完成。"
                ));
            }
        }

        Map<String, Object> extracted = new LinkedHashMap<>();
        Map<String, Object> first = orderData.get(0);
        extracted.put("crmNumber", asText(first.get("CRM项目编码")));
        extracted.put("projectName", asText(first.get("项目名称")));
        extracted.put("salesDept", asText(first.get("部门名称")));
        extracted.put("region", asText(first.get("区域")));
        extracted.put("kitDate", asText(first.get("齐套日期")));
        extracted.put("customerType", asText(first.get("客户类型")));
        extracted.put("orderAmount", first.getOrDefault("订单金额", 0));
        extracted.put("orderType", asText(first.get("订单类型")));
        extracted.put("marketCodeNameList", collect(orderData, "市场代码名称"));
        extracted.put("unshippedQtyList", collectNumber(orderData, "未发货数量"));
        extracted.put("expectDeliveryDateList", expectDeliveryDateList);
        extracted.put("agreedDeliveryOdcList", agreedDeliveryOdcList);
        extracted.put("materialDescList", collect(orderData, "物料描述"));
        extracted.put("settlementAmountCnyList", collectNumber(orderData, "结算金额(人民币)"));
        extracted.put("orderLineList", collect(orderData, "订单行"));
        extracted.put("supplyCoordinatorList", collect(orderData, "供应统筹"));
        extracted.put("orderPlaceDateList", orderPlaceDateList);

        @SuppressWarnings("unchecked")
        Map<String, String> aiPayload = body.get("aiPayload") instanceof Map<?, ?>
                ? ((Map<?, ?>) body.get("aiPayload")).entrySet().stream().collect(Collectors.toMap(
                x -> String.valueOf(x.getKey()), x -> String.valueOf(x.getValue()), (a, b) -> b))
                : Map.of();

        List<Map<String, Object>> productArray = buildProductArray(orderData, emptySearchResult, hasTodayOrder, orderType, asText(first.get("部门名称")), aiPayload);
        Map<String, String> coordinatorResult = resolveRegionCoordinator(asText(first.get("部门名称")), asText(first.get("区域")), asText(first.get("项目名称")));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("contractNo", contractNo);
        result.put("orderType", orderType);
        result.put("orderData", orderData);
        result.put("orderPlaceDateList", orderPlaceDateList);
        result.put("expectDeliveryDateList", expectDeliveryDateList);
        result.put("agreedDeliveryOdcList", agreedDeliveryOdcList);
        result.put("odcEmptyStatus", odcEmptyStatus);
        result.put("hasTodayOrder", hasTodayOrder);
        result.put("emptySearchResult", emptySearchResult);
        result.put("extracted", extracted);
        result.put("productArray", productArray);
        result.put("projectUrgency", aiPayload.getOrDefault("projectUrgency", "一般"));
        result.put("latestArrivalTime", aiPayload.getOrDefault("latestArrivalTime", ""));
        result.put("acceptPartialShipment", aiPayload.getOrDefault("acceptPartialShipment", "否"));
        result.put("mostUrgentProductList", aiPayload.getOrDefault("mostUrgentProductList", ""));
        result.put("delayedDeliveryImpact", aiPayload.getOrDefault("delayedDeliveryImpact", ""));
        result.put("coordinator", coordinatorResult);
        return result;
    }

    public boolean hasDuplicate(String contractNo) {
        String normalized = contractNo == null ? "" : contractNo.replaceAll("\\s+", "");
        return urgentRecords.stream().anyMatch(x -> normalized.equals(String.valueOf(x.getOrDefault("合同编号", ""))));
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

    public Map<String, String> submit(Map<String, Object> body, String sysId, String signServerAuth, String feishuAuthorization) {
        verifyAuthHeaders(sysId, signServerAuth);
        String taskNo = "DDJJ" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + (int) (Math.random() * 10);
        String requester = value(body, "requesterName");
        String contractNo = value(body, "contractNo").replaceAll("\\s+", "");
        String duplicateFlag = value(body, "duplicateFlag");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> productArray = body.get("productArray") instanceof List<?> list
                ? list.stream().filter(Map.class::isInstance).map(x -> (Map<String, Object>) x).collect(Collectors.toList())
                : List.of();

        if (productArray.isEmpty()) {
            throw new BusinessException(2004, "提交失败，未选择有效订单行");
        }

        for (Map<String, Object> product : productArray) {
            Map<String, Object> record = new LinkedHashMap<>();
            record.put("加急任务编号", taskNo);
            record.put("任务创建时间", LocalDateTime.now().format(DATETIME));
            record.put("任务提交人", requester);
            record.put("是否重复提交", duplicateFlag.isBlank() ? "否" : duplicateFlag);
            record.putAll(product);
            record.put("统筹评估回复", "");
            record.put("任务评估状态", "未评估");
            record.put("任务完成时间", "");
            record.put("任务修改时间", "");
            record.put("用户操作", "任务催办");
            record.put("区域统筹操作", "任务催办");
            urgentRecords.add(record);
        }

        boolean followResult = followOrderDeliveryDateChange(contractNo, value(body, "regionalCoordinatorFeishuId"), feishuAuthorization);
        return Map.of("taskNo", taskNo, "contractNo", contractNo, "followResult", String.valueOf(followResult));
    }

    public Map<String, Object> queryManagement(Integer pageNo, Integer pageSize,
                                               String contractNo, String orderNo, String projectName, String createdAt) {
        List<Map<String, Object>> filtered = urgentRecords.stream()
                .filter(x -> contains(x.get("合同编号"), contractNo))
                .filter(x -> contains(x.get("订单编号"), orderNo))
                .filter(x -> contains(x.get("项目名称"), projectName))
                .filter(x -> contains(x.get("任务创建时间"), createdAt))
                .sorted(Comparator.comparing(x -> String.valueOf(x.getOrDefault("任务创建时间", "")), Comparator.reverseOrder()))
                .collect(Collectors.toList());

        int no = pageNo == null || pageNo < 1 ? 1 : pageNo;
        int size = pageSize == null || pageSize < 1 ? 10 : pageSize;
        int from = (no - 1) * size;
        List<Map<String, Object>> page = from >= filtered.size()
                ? List.of()
                : filtered.subList(from, Math.min(filtered.size(), from + size));
        return Map.of("list", page, "total", filtered.size());
    }

    public Boolean saveEvaluation(String taskNo, String reply) {
        List<Map<String, Object>> rows = urgentRecords.stream()
                .filter(x -> taskNo.equals(x.get("加急任务编号")))
                .collect(Collectors.toList());
        if (rows.isEmpty()) {
            throw new BusinessException(2005, "任务不存在");
        }
        String now = LocalDateTime.now().format(DATETIME);
        for (Map<String, Object> row : rows) {
            String old = asText(row.get("统筹评估回复"));
            row.put("统筹评估回复", reply == null ? "" : reply);
            row.put("任务评估状态", reply == null || reply.isBlank() ? "未评估" : "已评估");
            if (reply != null && !reply.isBlank()) {
                row.put("任务完成时间", now);
                if (!old.equals(reply)) {
                    row.put("任务修改时间", now);
                }
            } else {
                row.put("任务完成时间", "");
                row.put("任务修改时间", "");
            }
        }
        return Boolean.TRUE;
    }

    private List<Map<String, Object>> buildProductArray(List<Map<String, Object>> orderData,
                                                         List<Map<String, String>> emptySearchResult,
                                                         boolean todayOrder,
                                                         String orderType,
                                                         String salesDept,
                                                         Map<String, String> aiPayload) {
        Map<String, String> reasonMap = emptySearchResult.stream().collect(Collectors.toMap(
                x -> x.get("orderNum") + "#" + x.get("lineNumber"),
                x -> x.getOrDefault("reason", ""),
                (a, b) -> b));

        boolean allMarketCodesEmpty = orderData.stream().allMatch(item -> asText(item.get("市场代码名称")).isBlank());
        boolean allVirtual = orderData.stream().allMatch(item -> "Y".equalsIgnoreCase(asText(item.get("是否虚拟"))));
        boolean keepAllByVirtual = allMarketCodesEmpty && allVirtual;

        List<Map<String, Object>> rows = new ArrayList<>();
        for (Map<String, Object> item : orderData) {
            String marketCode = asText(item.get("市场代码名称"));
            String virtualFlag = asText(item.get("是否虚拟"));
            if (!keepAllByVirtual && marketCode.isBlank()) {
                continue;
            }

            String odc = asText(item.get("约定交期-ODC"));
            String expect = asText(item.get("客户期望交期"));
            String orderNum = asText(item.get("订单编号"));
            String line = asText(item.get("订单行"));
            String emptyReason;
            if (!odc.isBlank()) {
                emptyReason = "";
            } else if (todayOrder && marketCode.isBlank()) {
                emptyReason = "无实物，待系统自动运算";
            } else if (todayOrder) {
                emptyReason = reasonMap.getOrDefault(orderNum + "#" + line, "资源不足，待统筹维护");
            } else if ("SMB 分销单".equals(orderType)) {
                emptyReason = "资源不足，待统筹维护";
            } else {
                emptyReason = compareReason(odc, expect);
            }

            boolean selectable = shouldSelectByScenario(salesDept, orderType, odc, expect);

            Map<String, Object> row = new LinkedHashMap<>(item);
            row.put("交期空白原因", emptyReason);
            row.put("项目紧急性", aiPayload.getOrDefault("projectUrgency", "一般"));
            row.put("最迟到货时间", aiPayload.getOrDefault("latestArrivalTime", ""));
            row.put("是否接受分批发货", aiPayload.getOrDefault("acceptPartialShipment", "否"));
            row.put("最紧急的商品清单", aiPayload.getOrDefault("mostUrgentProductList", ""));
            row.put("不及时交付的影响", aiPayload.getOrDefault("delayedDeliveryImpact", ""));
            row.put("selected", selectable);
            row.put("可勾选", selectable);
            row.put("代理产品统筹", matchAgentByModel(marketCode));
            rows.add(row);
        }

        rows.sort((a, b) -> {
            String odcA = asText(a.get("约定交期-ODC"));
            String odcB = asText(b.get("约定交期-ODC"));
            boolean emptyA = odcA.isBlank();
            boolean emptyB = odcB.isBlank();
            if (emptyA != emptyB) {
                return emptyA ? -1 : 1;
            }
            if (emptyA) {
                return 0;
            }
            return odcB.compareTo(odcA);
        });
        return rows;
    }

    private boolean shouldSelectByScenario(String salesDept, String orderType, String odc, String expect) {
        String dept = normalize(salesDept);
        boolean isSbg = dept.contains("sbg");
        LocalDate today = LocalDate.now();
        LocalDate odcDate = parseDateSafe(odc);
        LocalDate expectDate = parseDateSafe(expect);

        if (isSbg && "SMB 分销单".equals(orderType)) {
            if (odcDate == null) {
                return true;
            }
            return odcDate.isAfter(today);
        }

        if (odcDate == null) {
            return true;
        }
        if (expectDate == null) {
            return false;
        }
        return odcDate.isAfter(expectDate) && odcDate.isAfter(today);
    }

    private LocalDate parseDateSafe(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(raw, DATE);
        } catch (Exception ex) {
            return null;
        }
    }

    private String compareReason(String odc, String expect) {
        if (odc == null || odc.isBlank() || expect == null || expect.isBlank()) {
            return "资源不足，待统筹维护";
        }
        LocalDate d1 = LocalDate.parse(odc, DATE);
        LocalDate d2 = LocalDate.parse(expect, DATE);
        long days = d1.toEpochDay() - d2.toEpochDay();
        return days > 30 ? "客期大于30天，暂无法承诺交期" : "资源不足，待统筹维护";
    }

    private Map<String, String> resolveRegionCoordinator(String salesDept, String region, String projectName) {
        String dept = normalize(salesDept);
        String reg = normalize(region);
        String project = normalize(projectName);

        List<Map<String, String>> rules = List.of(
                rule("ebg", "深圳", "", "张某某", "ou_region_zhang", "张代理"),
                rule("cbg", "", "教育", "李某某", "ou_region_li", "李代理"),
                rule("tbu", "", "政企", "王某某", "ou_region_wang", "王代理"),
                rule("智能产业中心", "", "", "赵某某", "ou_region_zhao", "赵代理"),
                rule("sbg", "", "", "黄某某", "ou_region_huang", "黄代理")
        );

        for (Map<String, String> rule : rules) {
            if (!dept.contains(rule.get("dept"))) {
                continue;
            }
            String deptKeyword = normalize(rule.get("deptKeyword"));
            String projectKeyword = normalize(rule.get("projectKeyword"));
            String regionKeyword = normalize(rule.get("region"));
            boolean match = (!deptKeyword.isBlank() && dept.contains(deptKeyword))
                    || (!projectKeyword.isBlank() && project.contains(projectKeyword))
                    || (!regionKeyword.isBlank() && reg.contains(regionKeyword))
                    || (deptKeyword.isBlank() && projectKeyword.isBlank() && regionKeyword.isBlank());
            if (match) {
                boolean onLeave = "张某某".equals(rule.get("name"));
                return Map.of(
                        "name", rule.get("name"),
                        "feishuId", rule.get("feishuId"),
                        "agentName", onLeave ? rule.get("agentName") : ""
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
                "deptKeyword", region,
                "name", name,
                "feishuId", feishuId,
                "agentName", agentName
        );
    }

    private List<Map<String, Object>> mockOrderData(String contractNo) {
        if (!contractNo.matches("[A-Za-z0-9-]{6,}")) {
            return List.of();
        }
        String today = LocalDate.now().format(DATE);
        return List.of(
                mapOf(
                        "合同编号", contractNo,
                        "订单编号", "11101117663",
                        "订单行", "1.1",
                        "供应统筹", "",
                        "商务助理名称", "黄某某",
                        "商务助理员工ID", "ou_abda0d6bc814be1fcd7318a85bf3c1e7",
                        "约定交期-ODC", "",
                        "CRM项目编码", "20251010000225",
                        "项目名称", "某某项目",
                        "市场代码名称", "RG-SF2920U",
                        "未发货数量", 3,
                        "部门名称", "平台管理部",
                        "区域", "广东",
                        "齐套日期", "",
                        "客户类型", "代理商",
                        "客户期望交期", today,
                        "下单日期", today,
                        "结算金额(人民币)", 0,
                        "订单金额", 4000,
                        "物料描述", "基础质保服务",
                        "订单类型", "非特价下单",
                        "业务员姓名", "赵某某",
                        "最新风险等级", "",
                        "是否闭环", "",
                        "是否虚拟", "Y"
                ),
                mapOf(
                        "合同编号", contractNo,
                        "订单编号", "11101117663",
                        "订单行", "4.1",
                        "供应统筹", "王某某",
                        "商务助理名称", "黄某某",
                        "商务助理员工ID", "ou_abda0d6bc814be1fcd7318a85bf3c1e7",
                        "约定交期-ODC", LocalDate.now().plusDays(2).format(DATE),
                        "CRM项目编码", "20251010000225",
                        "项目名称", "某某项目",
                        "市场代码名称", "RG-MF2920E",
                        "未发货数量", 5,
                        "部门名称", "平台管理部",
                        "区域", "广东",
                        "齐套日期", "",
                        "客户类型", "代理商",
                        "客户期望交期", today,
                        "下单日期", today,
                        "结算金额(人民币)", 800,
                        "订单金额", 4000,
                        "物料描述", "出厂整机",
                        "订单类型", "非特价下单",
                        "业务员姓名", "赵某某",
                        "最新风险等级", "",
                        "是否闭环", "",
                        "是否虚拟", "N"
                )
        );
    }

    private String matchAgentByModel(String model) {
        if (model == null || model.isBlank()) {
            return "";
        }
        if (model.contains("MF")) {
            return "李代理";
        }
        if (model.contains("SF")) {
            return "王代理";
        }
        return "";
    }

    private String checkEmptyStatus(List<String> list) {
        long empty = list.stream().filter(x -> x == null || x.isBlank()).count();
        if (empty == list.size()) {
            return "全部为空";
        }
        if (empty == 0) {
            return "全部不为空";
        }
        return "部分为空";
    }

    private List<String> collect(List<Map<String, Object>> source, String key) {
        return source.stream().map(x -> asText(x.get(key))).collect(Collectors.toList());
    }

    private List<Number> collectNumber(List<Map<String, Object>> source, String key) {
        return source.stream().map(x -> x.get(key) instanceof Number n ? n : 0).collect(Collectors.toList());
    }

    private String normalize(String text) {
        return text == null ? "" : text.toLowerCase().replaceAll("\\s+", "");
    }

    private String asText(Object value) {
        if (value == null) {
            return "";
        }
        String text = String.valueOf(value);
        return "null".equalsIgnoreCase(text) ? "" : text;
    }

    private boolean contains(Object source, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return true;
        }
        return String.valueOf(source == null ? "" : source).contains(keyword.trim());
    }

    private String value(Map<String, ?> body, String key) {
        if (body == null || body.get(key) == null) {
            return "";
        }
        return String.valueOf(body.get(key));
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
            throw new BusinessException(2000, "签名生成失败");
        }
    }

    private boolean followOrderDeliveryDateChange(String contractNo, String regionalCoordinator, String feishuAuthorization) {
        if (contractNo == null || contractNo.isBlank()) {
            return false;
        }
        if (regionalCoordinator == null || regionalCoordinator.isBlank()) {
            return false;
        }
        return feishuAuthorization != null;
    }

    private void verifyAuthHeaders(String sysId, String signServerAuth) {
        if (sysId == null || sysId.isBlank() || signServerAuth == null || signServerAuth.isBlank()) {
            throw new BusinessException(2006, "请求头缺少 sysId 或 sign-server-auth");
        }
        String[] parts = signServerAuth.split("\\|");
        if (parts.length != 3) {
            throw new BusinessException(2007, "sign-server-auth 格式不合法");
        }
        long ts;
        try {
            ts = Long.parseLong(parts[1]);
        } catch (Exception ex) {
            throw new BusinessException(2007, "sign-server-auth 格式不合法");
        }
        if (System.currentTimeMillis() - ts > 5 * 60 * 1000L) {
            throw new BusinessException(2008, "signature 已过期，请重新获取");
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
