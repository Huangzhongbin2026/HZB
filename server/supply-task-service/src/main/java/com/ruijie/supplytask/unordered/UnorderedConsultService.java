package com.ruijie.supplytask.unordered;

import com.ruijie.supplytask.common.BusinessException;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

@Service
public class UnorderedConsultService {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final Map<String, Long> signatureCache = new HashMap<>();
    private final List<Map<String, Object>> managementRecords = new CopyOnWriteArrayList<>();
    private final Set<String> virtualProducts = new HashSet<>(Set.of("RG-VIRTUAL-100", "RG-VIRTUAL-200", "RG-WALL999"));

    public Map<String, String> generateSignature(Map<String, String> body) {
        String sysId = value(body, "sys_id");
        String secret = value(body, "access_key_secret");
        if (sysId.isBlank() || secret.isBlank()) {
            throw new BusinessException(2101, "sys_id 或 access_key_secret 不能为空");
        }
        long timestamp = System.currentTimeMillis();
        String digest = md5(sysId + timestamp + secret).toUpperCase();
        String signature = sysId + "|" + timestamp + "|" + digest;
        signatureCache.put(signature, timestamp);
        return Map.of("signature", signature);
    }

    public Map<String, Object> analyzeStep1(Map<String, Object> body, String sysId, String signServerAuth) {
        verifyAuthHeaders(sysId, signServerAuth);
        String crmNo = cleanSpecial(value(body, "crmNo").trim());
        String productInfo = cleanRawProductInfo(value(body, "productInfo"));
        String needTime = value(body, "needTime").trim();
        String remark = value(body, "remark").trim();

        if (!crmNo.isBlank() && !productInfo.isBlank()) {
            throw new BusinessException(2102, "CRM编号和产品型号两个只能填写一个，请重新编辑！");
        }
        if (crmNo.isBlank() && productInfo.isBlank()) {
            throw new BusinessException(2103, "CRM编号和产品型号二个必须填一个");
        }
        if (needTime.isBlank() || remark.isBlank()) {
            throw new BusinessException(2104, "需求时间和备注必填");
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("needTime", needTime);
        result.put("remark", remark);

        if (!crmNo.isBlank()) {
            result.put("judge", "CRM");
            result.put("crmNumber", crmNo);
            result.put("productList", List.of());
            return result;
        }

        List<String> lines = Arrays.stream(productInfo.split("\\r?\\n"))
                .map(this::cleanSpecial)
                .map(String::trim)
                .filter(x -> !x.isBlank())
                .toList();
        if (lines.isEmpty()) {
            throw new BusinessException(2105, "产品信息格式错误：至少包含一行有效产品型号和数量");
        }

        List<String> errors = new ArrayList<>();
        List<Map<String, Object>> products = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            String row = lines.get(i);
            ParseLine parsed = parseProductLine(row);
            if (!parsed.valid()) {
                errors.add("第" + (i + 1) + "行(" + row + ")：" + parsed.error());
                continue;
            }
            products.add(Map.of("产品型号", parsed.model(), "数量", parsed.quantity()));
        }

        if (!errors.isEmpty()) {
            throw new BusinessException(2106, "产品信息格式错误：" + String.join("；", errors));
        }

        result.put("judge", "PRODUCT");
        result.put("crmNumber", "");
        result.put("productList", products);
        return result;
    }

    public Map<String, Object> queryPlanByCrm(String crmNumber, String sysId, String signServerAuth) {
        verifyAuthHeaders(sysId, signServerAuth);
        String crm = cleanSpecial(crmNumber.trim());
        if (crm.isBlank()) {
            throw new BusinessException(2107, "您提交的CRM编号有误，请重新输入CRM编号！");
        }

        List<Map<String, Object>> records = mockPlanData(crm);
        if (records.isEmpty()) {
            throw new BusinessException(2107, "您提交的CRM编号有误，请重新输入CRM编号！");
        }
        boolean allZero = records.stream().allMatch(x -> toInt(x.get("数量")) == 0);
        if (allZero) {
            throw new BusinessException(2108, "CRM编号对应的产品型号的调整数量全部为“0”，此单已无数量需进行评估！");
        }

        List<Map<String, Object>> table = records.stream()
                .filter(x -> !asText(x.get("产品型号")).isBlank())
                .map(x -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("selected", true);
                    row.put("CRM编号", crm);
                    row.put("产品型号", asText(x.get("产品型号")));
                    row.put("产品名称", asText(x.get("产品名称")));
                    row.put("数量", toInt(x.get("数量")));
                    row.put("要求发货日期", asText(x.get("客期")));
                    row.put("是否备货", asText(x.get("是否备货")));
                    row.put("勾选或取消备货时间", asText(x.get("勾选或取消备货时间")));
                    row.put("项目盘点详情url", "https://marketplan.ruijie.com.cn/theprojectinventoryDetail?userid=" + crm);
                    row.put("一级部门", asText(x.get("一级部门")));
                    row.put("三级部门", asText(x.get("三级部门")));
                    row.put("机会所有人", asText(x.get("机会所有人")));
                    row.put("项目名称", asText(x.get("业务机会名称")));
                    row.put("客户名称", asText(x.get("客户名称")));
                    row.put("是否退市报备", asText(x.get("是否退市报备")));
                    row.put("产品流速", asText(x.get("产品流速")));
                    return row;
                })
                .toList();

        return Map.of(
                "盘点记录", records,
                "项目盘点详情url", "https://marketplan.com.cn/userid=" + crm,
                "tableData", table
        );
    }

    public Map<String, Object> queryTodayDuplicate(String crmNumber) {
        String crm = cleanSpecial(crmNumber.trim());
        String today = LocalDate.now().format(DATE);
        List<Map<String, Object>> rows = managementRecords.stream()
                .filter(x -> crm.equals(asText(x.get("CRM编号"))))
                .filter(x -> asText(x.get("任务创建时间")).startsWith(today))
                .filter(x -> "已评估".equals(asText(x.get("任务评估状态"))))
                .map(x -> mapOf(
                        "CRM编号", asText(x.get("CRM编号")),
                        "产品型号", asText(x.get("产品型号")),
                        "数量", x.getOrDefault("数量", 0),
                        "客户期望日期", asText(x.get("客户期望日期")),
                        "需求日期", asText(x.get("需求日期")),
                        "备注", asText(x.get("备注")),
                        "评估预计交期", asText(x.get("统筹评估回复"))
                ))
                .toList();
        return Map.of("hasDuplicate", !rows.isEmpty(), "rows", rows);
    }

    public Boolean joinTodayDuplicate(String crmNumber, String collaborator) {
        String crm = cleanSpecial(crmNumber == null ? "" : crmNumber.trim());
        if (crm.isBlank()) {
            return Boolean.FALSE;
        }
        String name = collaborator == null ? "" : collaborator.trim();
        managementRecords.stream()
                .filter(x -> crm.equals(asText(x.get("CRM编号"))))
                .forEach(x -> x.put("协作人", name));
        return Boolean.TRUE;
    }

    public Map<String, Object> queryHistoryDuplicate(String crmNumber) {
        String crm = cleanSpecial(crmNumber.trim());
        String today = LocalDate.now().format(DATE);
        boolean exists = managementRecords.stream()
                .filter(x -> crm.equals(asText(x.get("CRM编号"))))
                .anyMatch(x -> !asText(x.get("任务创建时间")).startsWith(today));
        return Map.of("hasHistoryDuplicate", exists);
    }

    public Map<String, Object> submit(Map<String, Object> body, String sysId, String signServerAuth) {
        verifyAuthHeaders(sysId, signServerAuth);
        String mode = value(body, "mode");
        String requester = value(body, "requesterName");
        String crmNumber = cleanSpecial(value(body, "crmNumber"));
        String needTime = value(body, "needTime");
        String remark = value(body, "remark");
        String taskType = value(body, "taskType");
        String repeatFlag = value(body, "repeatFlag");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> selectedData = body.get("selectedData") instanceof List<?> list
                ? list.stream().filter(Map.class::isInstance).map(x -> (Map<String, Object>) x).collect(Collectors.toList())
                : List.of();

        if (selectedData.isEmpty()) {
            throw new BusinessException(2109, "没有可提交的数据，请至少选择一条记录");
        }

        List<Map<String, Object>> normalized = new ArrayList<>();
        List<String> invalidModels = new ArrayList<>();
        for (Map<String, Object> item : selectedData) {
            String model = cleanSpecial(asText(item.get("产品型号")));
            if (model.isBlank()) {
                continue;
            }
            String planner = queryPlanner(model);
            if (planner.isBlank() && "PRODUCT".equalsIgnoreCase(mode)) {
                boolean passVirtual = virtualProducts.contains(model);
                boolean passRule = countToken(model, "RG-") == 1;
                if (!passVirtual && !passRule) {
                    invalidModels.add(model);
                    continue;
                }
            }

            Map<String, Object> row = new LinkedHashMap<>(item);
            row.put("产品型号", model);
            row.put("产品统筹", planner);
            normalized.add(row);
        }

        if (!invalidModels.isEmpty() && invalidModels.size() == selectedData.size()) {
            throw new BusinessException(2110, "您提交的全部产品型号无法查询到系统数据及格式不合法，无法提交任务！");
        }
        if (!invalidModels.isEmpty()) {
            throw new BusinessException(2111, "您提交的部分产品型号无法查询到系统数据及格式不合法，无法提交任务！具体型号如下：" + String.join("、", invalidModels));
        }

        String taskNo = "WXD" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + (int) (Math.random() * 10);
        String now = LocalDateTime.now().format(DATETIME);

        for (Map<String, Object> row : normalized) {
            String model = asText(row.get("产品型号"));
            Integer qty = toInt(row.get("数量"));
            Map<String, Object> historyMap = buildHistoryFields(crmNumber, model, "是".equals(repeatFlag));
            Map<String, Object> record = new LinkedHashMap<>();
            record.put("交期咨询任务编号", taskNo);
            record.put("任务创建时间", now);
            record.put("任务创建日期", LocalDate.now().format(DATE));
            record.put("产品型号", model);
            record.put("产品名称", asText(row.get("产品名称")));
            record.put("数量", qty);
            record.put("客户期望日期", asText(row.get("要求发货日期")));
            record.put("是否备货", asText(row.get("是否备货")));
            record.put("产品统筹", asText(row.get("产品统筹")));
            record.put("代理产品统筹", matchAgentPlanner(model, asText(row.get("产品统筹"))));
            record.put("项目盘点详情url", asText(row.get("项目盘点详情url")));
            record.put("提问人", requester);
            record.put("CRM编号", crmNumber);
            record.put("备注", remark);
            record.put("勾选或取消备货时间", asText(row.get("勾选或取消备货时间")));
            record.put("需求日期", needTime);
            record.put("需求日期时间戳", toEpochMilli(needTime));
            record.put("一级部门", asText(row.get("一级部门")));
            record.put("三级部门", asText(row.get("三级部门")));
            record.put("机会所有人", asText(row.get("机会所有人")));
            record.put("项目名称", asText(row.get("项目名称")));
            record.put("客户名称", asText(row.get("客户名称")));
            record.put("是否退市报备", asText(row.get("是否退市报备")));
            record.put("任务类型", taskType.isBlank() ? "普通项目" : taskType);
            record.put("是否重复提交", repeatFlag.isBlank() ? "否" : repeatFlag);
            record.put("产品流速", asText(row.get("产品流速")));
            record.put("问题类型", "PRODUCT".equalsIgnoreCase(mode) ? "产品型号直接提问" : "CRM项目提问");
            record.put("协作人", "");
            record.put("统筹评估回复", "");
            record.put("任务评估状态", "未评估");
            record.put("任务完成时间", "");
            record.put("任务修改时间", "");
            record.put("任务完成时效", "");
            record.put("用户操作", "任务催办");
            record.put("评估历史", new ArrayList<Map<String, Object>>());
            record.putAll(historyMap);
            managementRecords.add(record);
        }

        return Map.of("taskNo", taskNo, "count", normalized.size());
    }

    public Map<String, Object> queryManagement(Integer pageNo, Integer pageSize,
                                               String taskNo, String crmNumber, String productModel, String createdAt) {
        List<Map<String, Object>> list = managementRecords.stream()
                .filter(x -> contains(x.get("交期咨询任务编号"), taskNo))
                .filter(x -> contains(x.get("CRM编号"), crmNumber))
                .filter(x -> contains(x.get("产品型号"), productModel))
                .filter(x -> contains(x.get("任务创建时间"), createdAt))
                .sorted(Comparator.comparing(x -> asText(x.get("任务创建时间")), Comparator.reverseOrder()))
                .toList();

        int no = pageNo == null || pageNo < 1 ? 1 : pageNo;
        int size = pageSize == null || pageSize < 1 ? 10 : pageSize;
        int from = (no - 1) * size;
        List<Map<String, Object>> page = from >= list.size() ? List.of() : list.subList(from, Math.min(from + size, list.size()));
        return Map.of("list", page, "total", list.size());
    }

    public Boolean saveEvaluation(String taskNo, String productModel, String reply, Integer quantity) {
        List<Map<String, Object>> rows = managementRecords.stream()
                .filter(x -> taskNo.equals(asText(x.get("交期咨询任务编号"))))
                .filter(x -> productModel == null || productModel.isBlank() || productModel.equals(asText(x.get("产品型号"))))
                .collect(Collectors.toList());
        if (rows.isEmpty()) {
            throw new BusinessException(2112, "任务不存在");
        }
        String now = LocalDateTime.now().format(DATETIME);
        for (Map<String, Object> row : rows) {
            String old = asText(row.get("统筹评估回复"));
            String newReply = reply == null ? "" : reply;
            row.put("统筹评估回复", newReply);
            row.put("任务评估状态", newReply.isBlank() ? "未评估" : "已评估");

            if (!newReply.isBlank()) {
                row.put("任务完成时间", now);
                String created = asText(row.get("任务创建时间"));
                row.put("任务完成时效", calcHours(created, now));
                if (!old.equals(newReply)) {
                    row.put("任务修改时间", now);
                }
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> history = (List<Map<String, Object>>) row.get("评估历史");
                history.add(0, mapOf("time", now, "quantity", quantity == null ? row.get("数量") : quantity, "reply", newReply));
                if (history.size() > 3) {
                    history.subList(3, history.size()).clear();
                }
            } else {
                row.put("任务完成时间", "");
                row.put("任务修改时间", "");
                row.put("任务完成时效", "");
            }
        }
        return Boolean.TRUE;
    }

    public Boolean urge(String taskNo) {
        return managementRecords.stream().anyMatch(x -> taskNo.equals(asText(x.get("交期咨询任务编号"))));
    }

    private Map<String, Object> buildHistoryFields(String crmNumber, String productModel, boolean repeated) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("最新评估时间", "");
        result.put("最新评估数量", "");
        result.put("最新评估统筹回复", "");
        result.put("倒数第二次评估时间", "");
        result.put("倒数第二次评估数量", "");
        result.put("倒数第二次评估统筹回复", "");
        result.put("倒数第三次评估时间", "");
        result.put("倒数第三次评估数量", "");
        result.put("倒数第三次评估统筹回复", "");

        if (!repeated || crmNumber.isBlank()) {
            return result;
        }

        List<Map<String, Object>> history = managementRecords.stream()
                .filter(x -> crmNumber.equals(asText(x.get("CRM编号"))))
                .filter(x -> productModel.equals(asText(x.get("产品型号"))))
                .filter(x -> !asText(x.get("统筹评估回复")).isBlank())
                .sorted(Comparator.comparing(x -> asText(x.get("任务创建时间")), Comparator.reverseOrder()))
                .limit(3)
                .toList();

        if (history.size() > 0) {
            fillHistory(result, history.get(0), "最新评估");
        }
        if (history.size() > 1) {
            fillHistory(result, history.get(1), "倒数第二次评估");
        }
        if (history.size() > 2) {
            fillHistory(result, history.get(2), "倒数第三次评估");
        }
        return result;
    }

    private void fillHistory(Map<String, Object> target, Map<String, Object> source, String prefix) {
        target.put(prefix + "时间", asText(source.get("任务创建时间")));
        target.put(prefix + "数量", source.getOrDefault("数量", ""));
        target.put(prefix + "统筹回复", asText(source.get("统筹评估回复")));
    }

    private String queryPlanner(String model) {
        // 模拟第一种 marketName 查询
        if (model.startsWith("RG-") || model.contains("MM850") || model.contains("QSFP")) {
            return "李某某";
        }
        // 模拟第二种 productModel 查询兜底
        if (model.endsWith("-ALT") || model.startsWith("WALL")) {
            return "王某某";
        }
        return "";
    }

    private String matchAgentPlanner(String model, String planner) {
        if (model.contains("WALL") && !planner.isBlank()) {
            return planner + "(代理)";
        }
        return "";
    }

    private List<Map<String, Object>> mockPlanData(String crm) {
        if (crm.startsWith("000") || crm.length() < 6) {
            return List.of();
        }
        if (crm.startsWith("999")) {
            return List.of(
                    mapOf("CRM编号", crm, "产品型号", "RG-IF2920U", "产品名称", "维保服务", "数量", 0, "客期", LocalDate.now().plusDays(10).format(DATE),
                            "是否备货", "是", "勾选或取消备货时间", LocalDate.now().minusDays(2).format(DATE), "一级部门", "平台管理部", "三级部门", "广东区域",
                            "机会所有人", "赵某某", "业务机会名称", "某某项目", "客户名称", "某某客户", "是否退市报备", "否", "产品流速", "")
            );
        }
        return List.of(
                mapOf("CRM编号", crm, "产品型号", "MINI-GBIC-LX-SM1310", "产品名称", "S5760-X系列", "数量", 24, "客期", "2026-04-30",
                        "是否备货", "是", "勾选或取消备货时间", "2026-03-15", "一级部门", "平台管理部", "三级部门", "广东区域",
                        "机会所有人", "赵某某", "业务机会名称", "某某项目", "客户名称", "某某客户", "是否退市报备", "否", "产品流速", "100"),
                mapOf("CRM编号", crm, "产品型号", "RG-CCP-DCP-LIC-EDU", "产品名称", "桌面计算平台授权", "数量", 72, "客期", "2026-04-30",
                        "是否备货", "是", "勾选或取消备货时间", "2026-03-15", "一级部门", "平台管理部", "三级部门", "广东区域",
                        "机会所有人", "赵某某", "业务机会名称", "某某项目", "客户名称", "某某客户", "是否退市报备", "否", "产品流速", "")
        );
    }

    private ParseLine parseProductLine(String row) {
        int splitAt = -1;
        for (int i = row.length() - 2; i >= 0; i--) {
            char c = row.charAt(i);
            if (c == '*' || Character.isWhitespace(c)) {
                splitAt = i;
                break;
            }
        }
        if (splitAt < 0 || splitAt >= row.length() - 1) {
            return ParseLine.error("缺少分隔符");
        }

        String model = row.substring(0, splitAt).trim();
        String qtyPart = row.substring(splitAt + 1).trim();
        if (model.isBlank()) {
            return ParseLine.error("产品型号为空");
        }
        Integer qty;
        try {
            qty = Integer.parseInt(qtyPart);
        } catch (Exception ex) {
            return ParseLine.error("数量必须为正数字");
        }
        if (qty <= 0) {
            return ParseLine.error("数量必须为正数字");
        }
        return ParseLine.success(model, qty);
    }

    private String cleanRawProductInfo(String text) {
        if (text == null) {
            return "";
        }
        return text.replaceAll("[^A-Za-z0-9\\u4e00-\\u9fa5\\-_*\\s\\r\\n]", "");
    }

    private String cleanSpecial(String text) {
        if (text == null) {
            return "";
        }
        return text.replaceAll("[^A-Za-z0-9\\u4e00-\\u9fa5\\-_*\\s]", "").trim();
    }

    private int countToken(String text, String token) {
        if (text == null || token == null || token.isEmpty()) {
            return 0;
        }
        int count = 0;
        int start = 0;
        while (true) {
            int idx = text.indexOf(token, start);
            if (idx < 0) {
                return count;
            }
            count++;
            start = idx + token.length();
        }
    }

    private void verifyAuthHeaders(String sysId, String signServerAuth) {
        if (sysId == null || sysId.isBlank() || signServerAuth == null || signServerAuth.isBlank()) {
            throw new BusinessException(2113, "请求头缺少 sysId 或 sign-server-auth");
        }
        String[] parts = signServerAuth.split("\\|");
        if (parts.length != 3) {
            throw new BusinessException(2114, "sign-server-auth 格式不合法");
        }
        long ts;
        try {
            ts = Long.parseLong(parts[1]);
        } catch (Exception ex) {
            throw new BusinessException(2114, "sign-server-auth 格式不合法");
        }
        if (System.currentTimeMillis() - ts > 5 * 60 * 1000L) {
            throw new BusinessException(2115, "signature 已过期，请重新获取");
        }
    }

    private String value(Map<String, ?> body, String key) {
        if (body == null || body.get(key) == null) {
            return "";
        }
        return String.valueOf(body.get(key));
    }

    private boolean contains(Object source, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return true;
        }
        return asText(source).contains(keyword.trim());
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

    private String calcHours(String start, String end) {
        if (start.isBlank() || end.isBlank()) {
            return "";
        }
        try {
            LocalDateTime s = LocalDateTime.parse(start, DATETIME);
            LocalDateTime e = LocalDateTime.parse(end, DATETIME);
            return String.valueOf(Math.max(0, Duration.between(s, e).toHours()));
        } catch (Exception ex) {
            return "";
        }
    }

    private Long toEpochMilli(String date) {
        if (date == null || date.isBlank()) {
            return 0L;
        }
        try {
            LocalDate d = LocalDate.parse(date, DATE);
            return d.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
        } catch (Exception ex) {
            return 0L;
        }
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
            throw new BusinessException(2116, "签名生成失败");
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

    private record ParseLine(boolean valid, String model, Integer quantity, String error) {
        static ParseLine success(String model, Integer quantity) {
            return new ParseLine(true, model, quantity, "");
        }

        static ParseLine error(String error) {
            return new ParseLine(false, "", 0, error);
        }
    }
}
