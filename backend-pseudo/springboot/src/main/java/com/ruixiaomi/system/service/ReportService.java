package com.ruixiaomi.system.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruixiaomi.system.dto.ReportDashboardConfigUpdateDTO;
import com.ruixiaomi.system.dto.TaskQueryDTO;
import com.ruixiaomi.system.entity.CustomerDueChangeTaskEntity;
import com.ruixiaomi.system.entity.DeliveryAssessmentTaskEntity;
import com.ruixiaomi.system.entity.ExpeditedTaskEntity;
import com.ruixiaomi.system.entity.ManualConsultSessionEntity;
import com.ruixiaomi.system.entity.ReportDashboardConfigEntity;
import com.ruixiaomi.system.exception.BusinessException;
import com.ruixiaomi.system.mapper.ManualConsultMapper;
import com.ruixiaomi.system.mapper.ReportMapper;
import com.ruixiaomi.system.security.AuthenticationFacade;
import com.ruixiaomi.system.security.CurrentUser;
import com.ruixiaomi.system.vo.LoginLogVO;
import com.ruixiaomi.system.vo.ReportDashboardConfigVO;
import com.ruixiaomi.system.vo.ReportDashboardVO;
import com.ruixiaomi.system.vo.ReportPendingDetailVO;
import com.ruixiaomi.system.vo.ReportWorkbenchVO;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReportService {

  private static final String CONFIG_KEY = "default";
  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
  private static final List<SectionSetting> DEFAULT_SECTIONS = List.of(
    new SectionSetting("taskOverview", "任务概览", 10, true),
    new SectionSetting("usageTrend", "平台使用趋势", 20, true),
    new SectionSetting("submitCompare", "发起与成功对比", 30, true),
    new SectionSetting("questionerTrend", "提问趋势", 40, true),
    new SectionSetting("completionAging", "完成时效趋势", 50, true),
    new SectionSetting("coordinatorEfficiency", "产品统筹时效", 60, true),
    new SectionSetting("regionalPending", "区域统筹待处理", 70, true),
    new SectionSetting("productPending", "产品统筹待处理", 80, true)
  );

  private final TaskService taskService;
  private final MetadataService metadataService;
  private final ManualConsultService manualConsultService;
  private final ManualConsultMapper manualConsultMapper;
  private final AuthenticationFacade authenticationFacade;
  private final ReportMapper reportMapper;
  private final ObjectMapper objectMapper;

  public ReportService(TaskService taskService,
                       MetadataService metadataService,
                       ManualConsultService manualConsultService,
                       ManualConsultMapper manualConsultMapper,
                       AuthenticationFacade authenticationFacade,
                       ReportMapper reportMapper,
                       ObjectMapper objectMapper) {
    this.taskService = taskService;
    this.metadataService = metadataService;
    this.manualConsultService = manualConsultService;
    this.manualConsultMapper = manualConsultMapper;
    this.authenticationFacade = authenticationFacade;
    this.reportMapper = reportMapper;
    this.objectMapper = objectMapper;
  }

  public ReportDashboardVO getDashboard(String range) {
    DateRange dateRange = resolveRange(range);
    List<ExpeditedTaskEntity> expeditedTasks = taskService.listExpeditedTasks(emptyQuery());
    List<DeliveryAssessmentTaskEntity> assessmentTasks = taskService.listDeliveryAssessmentTasks(emptyQuery());
    List<CustomerDueChangeTaskEntity> dueChangeTasks = taskService.listCustomerDueChangeTasks(emptyQuery());
    List<LoginLogVO> loginLogs = metadataService.listLoginLogs();
    List<ReportMapper.ReportRecordTrendRow> successRows = reportMapper.selectPortalTaskSuccessRows(dateRange.startDate(), dateRange.endDate());
    List<ReportMapper.ReportRecordTrendRow> startedRows = reportMapper.selectPortalTaskStartRows(dateRange.startDate(), dateRange.endDate());

    List<ReportDashboardVO.TaskOverview> taskOverviews = List.of(
      buildTaskOverview("EXPEDITED", "订单加急任务", expeditedTasks.stream().filter(task -> withinRange(task.getCreatedAt(), dateRange)).toList()),
      buildTaskOverview("ASSESSMENT", "未下单咨询任务", assessmentTasks.stream().filter(task -> withinRange(task.getCreatedAt(), dateRange)).toList()),
      buildTaskOverview("DUE_CHANGE", "客期变更任务", dueChangeTasks.stream().filter(task -> withinRange(task.getCreatedAt(), dateRange)).toList())
    );

    LinkedHashSet<String> activeQuestioners = new LinkedHashSet<>();
    successRows.forEach(row -> {
      if (!isBlank(row.getQuestionerName())) {
        activeQuestioners.add(row.getQuestionerName().trim());
      }
    });

    ReportDashboardVO.UsageOverview usageOverview = new ReportDashboardVO.UsageOverview(
      (int) loginLogs.stream().filter(log -> withinRange(log.time(), dateRange)).count(),
      activeQuestioners.size(),
      startedRows.size(),
      successRows.size()
    );

    return new ReportDashboardVO(
      dateRange.rangeKey(),
      dateRange.startDate(),
      dateRange.endDate(),
      LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
      usageOverview,
      taskOverviews,
      buildUsageTrend(dateRange, loginLogs, successRows),
      buildSubmitCompareTrend(dateRange, startedRows, successRows),
      buildQuestionerTrend(dateRange, successRows),
      buildCompletionAgingTrend(dateRange, expeditedTasks, assessmentTasks, dueChangeTasks),
      buildCoordinatorEfficiency(dateRange, expeditedTasks, assessmentTasks, dueChangeTasks),
      buildRegionalPending(expeditedTasks, dueChangeTasks),
      buildProductPending(expeditedTasks, assessmentTasks, dueChangeTasks)
    );
  }

  public ReportWorkbenchVO getWorkbench(String range) {
    DateRange dateRange = resolveRange(range);
    CurrentUser currentUser = authenticationFacade.requireCurrentUser();
    String currentUserName = normalizeText(currentUser.getDisplayName());

    List<ExpeditedTaskEntity> expeditedTasks = taskService.listExpeditedTasks(emptyQuery());
    List<DeliveryAssessmentTaskEntity> assessmentTasks = taskService.listDeliveryAssessmentTasks(emptyQuery());
    List<CustomerDueChangeTaskEntity> dueChangeTasks = taskService.listCustomerDueChangeTasks(emptyQuery());
    List<ManualConsultSessionEntity> consultSessions = manualConsultMapper.selectWorkbenchSessionsByUserName(currentUser.getDisplayName());

    List<ReportWorkbenchVO.PendingTaskVO> pendingTasks = buildPendingTasks(currentUserName, expeditedTasks, assessmentTasks, dueChangeTasks);
    List<ReportWorkbenchVO.UnreadConsultVO> unreadConsults = buildUnreadConsults(currentUserName, consultSessions);

    return new ReportWorkbenchVO(
      dateRange.rangeKey(),
      dateRange.startDate(),
      dateRange.endDate(),
      pendingTasks.size(),
      unreadConsults.size(),
      pendingTasks,
      unreadConsults,
      buildTaskTrend(dateRange, currentUserName, expeditedTasks, assessmentTasks, dueChangeTasks),
      buildChatTrend(dateRange, currentUserName, consultSessions)
    );
  }

  public List<ReportPendingDetailVO> listPendingDetails(String ownerType, String ownerName) {
    String normalizedOwnerType = normalizeText(ownerType);
    String normalizedOwnerName = normalizeText(ownerName);
    if (isBlank(normalizedOwnerType) || isBlank(normalizedOwnerName)) {
      throw new BusinessException(400, "待处理明细查询参数不能为空");
    }
    List<ReportPendingDetailVO> details = new ArrayList<>();
    if ("regional".equals(normalizedOwnerType)) {
      for (ExpeditedTaskEntity task : taskService.listExpeditedTasks(emptyQuery())) {
        if (isPendingToday(task.getCreatedAt(), task.getOverallTaskStatus(), task.getTaskEvalStatus(), task.getCompletedAt())
          && normalizedOwnerName.equals(normalizeText(task.getRegionalCoordinator()))) {
          details.add(new ReportPendingDetailVO("regional", ownerName, "EXPEDITED", "订单加急任务", task.getId(), task.getTaskNo(),
            firstNonBlank(task.getContractNo(), task.getOrderNo()), firstNonBlank(task.getQuestionerName(), "-"),
            firstNonBlank(task.getOverallTaskStatus(), task.getTaskEvalStatus(), "未完成"), task.getCreatedAt(), task.getCompletedAt(), decimalToDouble(task.getCompletionDurationHours())));
        }
      }
      for (CustomerDueChangeTaskEntity task : taskService.listCustomerDueChangeTasks(emptyQuery())) {
        if (isPendingToday(task.getCreatedAt(), task.getOverallTaskStatus(), task.getTaskEvalStatus(), task.getCompletedAt())
          && normalizedOwnerName.equals(normalizeText(task.getRegionalCoordinator()))) {
          details.add(new ReportPendingDetailVO("regional", ownerName, "DUE_CHANGE", "客期变更任务", task.getId(), task.getTaskNo(),
            firstNonBlank(task.getContractNo(), task.getOrderNo()), firstNonBlank(task.getQuestionerName(), "-"),
            firstNonBlank(task.getOverallTaskStatus(), task.getTaskEvalStatus(), "未完成"), task.getCreatedAt(), task.getCompletedAt(), decimalToDouble(task.getCompletionDurationHours())));
        }
      }
    } else if ("product".equals(normalizedOwnerType)) {
      for (ExpeditedTaskEntity task : taskService.listExpeditedTasks(emptyQuery())) {
        if (isPendingToday(task.getCreatedAt(), task.getOverallTaskStatus(), task.getTaskEvalStatus(), task.getCompletedAt())
          && normalizedOwnerName.equals(normalizeText(task.getProductCoordinator()))) {
          details.add(new ReportPendingDetailVO("product", ownerName, "EXPEDITED", "订单加急任务", task.getId(), task.getTaskNo(),
            firstNonBlank(task.getContractNo(), task.getOrderNo()), firstNonBlank(task.getQuestionerName(), "-"),
            firstNonBlank(task.getOverallTaskStatus(), task.getTaskEvalStatus(), "未完成"), task.getCreatedAt(), task.getCompletedAt(), decimalToDouble(task.getCompletionDurationHours())));
        }
      }
      for (DeliveryAssessmentTaskEntity task : taskService.listDeliveryAssessmentTasks(emptyQuery())) {
        if (isPendingToday(task.getCreatedAt(), task.getOverallTaskStatus(), task.getTaskEvalStatus(), task.getCompletedAt())
          && normalizedOwnerName.equals(normalizeText(task.getProductCoordinator()))) {
          details.add(new ReportPendingDetailVO("product", ownerName, "ASSESSMENT", "未下单咨询任务", task.getId(), task.getTaskNo(),
            firstNonBlank(task.getCrmNo(), task.getProductModel()), firstNonBlank(task.getQuestionerName(), "-"),
            firstNonBlank(task.getOverallTaskStatus(), task.getTaskEvalStatus(), "未完成"), task.getCreatedAt(), task.getCompletedAt(), decimalToDouble(task.getCompletionDurationHours())));
        }
      }
      for (CustomerDueChangeTaskEntity task : taskService.listCustomerDueChangeTasks(emptyQuery())) {
        if (isPendingToday(task.getCreatedAt(), task.getOverallTaskStatus(), task.getTaskEvalStatus(), task.getCompletedAt())
          && normalizedOwnerName.equals(normalizeText(task.getProductCoordinator()))) {
          details.add(new ReportPendingDetailVO("product", ownerName, "DUE_CHANGE", "客期变更任务", task.getId(), task.getTaskNo(),
            firstNonBlank(task.getContractNo(), task.getOrderNo()), firstNonBlank(task.getQuestionerName(), "-"),
            firstNonBlank(task.getOverallTaskStatus(), task.getTaskEvalStatus(), "未完成"), task.getCreatedAt(), task.getCompletedAt(), decimalToDouble(task.getCompletionDurationHours())));
        }
      }
    } else {
      throw new BusinessException(400, "不支持的待处理维度");
    }

    details.sort(Comparator.comparing(ReportPendingDetailVO::createdAt, Comparator.nullsLast(String::compareTo)).reversed());
    return details;
  }

  public ReportDashboardConfigVO getDashboardConfig() {
    return toConfigVO(loadOrCreateConfig());
  }

  @Transactional(rollbackFor = Exception.class)
  public ReportDashboardConfigVO updateDashboardConfig(ReportDashboardConfigUpdateDTO request) {
    ReportDashboardConfigEntity entity = loadOrCreateConfig();
    entity.setConfigName(firstNonBlank(normalizeText(request.configName()), "数据看板配置"));
    entity.setDefaultRange(normalizeRange(request.defaultRange()));
    entity.setSectionsJson(writeSections(mergeSections(request.sections())));
    reportMapper.updateDashboardConfig(entity);
    return toConfigVO(loadOrCreateConfig());
  }

  private ReportDashboardConfigEntity loadOrCreateConfig() {
    ReportDashboardConfigEntity entity = reportMapper.selectDashboardConfig(CONFIG_KEY);
    if (entity != null) {
      if (isBlank(entity.getSectionsJson())) {
        entity.setSectionsJson(writeSections(DEFAULT_SECTIONS));
        reportMapper.updateDashboardConfig(entity);
      }
      if (isBlank(entity.getDefaultRange())) {
        entity.setDefaultRange("week");
        reportMapper.updateDashboardConfig(entity);
      }
      return entity;
    }
    entity = new ReportDashboardConfigEntity();
    entity.setConfigKey(CONFIG_KEY);
    entity.setConfigName("数据看板配置");
    entity.setDefaultRange("week");
    entity.setSectionsJson(writeSections(DEFAULT_SECTIONS));
    reportMapper.insertDashboardConfig(entity);
    return entity;
  }

  private ReportDashboardConfigVO toConfigVO(ReportDashboardConfigEntity entity) {
    return new ReportDashboardConfigVO(
      firstNonBlank(entity.getConfigName(), "数据看板配置"),
      normalizeRange(entity.getDefaultRange()),
      readSections(entity.getSectionsJson()).stream()
        .sorted(Comparator.comparing(SectionSetting::sort))
        .map(section -> new ReportDashboardConfigVO.ReportDashboardSectionVO(section.key(), section.label(), section.sort(), section.enabled()))
        .toList()
    );
  }

  private List<ReportDashboardVO.TrendPoint> buildUsageTrend(DateRange range,
                                                             List<LoginLogVO> loginLogs,
                                                             List<ReportMapper.ReportRecordTrendRow> successRows) {
    Map<String, Integer> loginMap = initBucketCounts(range);
    Map<String, Integer> successMap = initBucketCounts(range);
    loginLogs.stream().filter(log -> withinRange(log.time(), range)).forEach(log -> incrementCount(loginMap, toDateText(log.time())));
    successRows.forEach(row -> incrementCount(successMap, row.getStatDate()));
    return range.dateKeys().stream()
      .map(date -> new ReportDashboardVO.TrendPoint(date, loginMap.getOrDefault(date, 0), successMap.getOrDefault(date, 0)))
      .toList();
  }

  private List<ReportDashboardVO.SubmitComparePoint> buildSubmitCompareTrend(DateRange range,
                                                                             List<ReportMapper.ReportRecordTrendRow> startedRows,
                                                                             List<ReportMapper.ReportRecordTrendRow> successRows) {
    Map<String, Integer> startedMap = initBucketCounts(range);
    Map<String, Integer> successMap = initBucketCounts(range);
    startedRows.forEach(row -> incrementCount(startedMap, row.getStatDate()));
    successRows.forEach(row -> incrementCount(successMap, row.getStatDate()));
    return range.dateKeys().stream()
      .map(date -> new ReportDashboardVO.SubmitComparePoint(date, startedMap.getOrDefault(date, 0), successMap.getOrDefault(date, 0)))
      .toList();
  }

  private List<ReportDashboardVO.QuestionerTrendPoint> buildQuestionerTrend(DateRange range,
                                                                            List<ReportMapper.ReportRecordTrendRow> successRows) {
    Map<String, Integer> counts = new LinkedHashMap<>();
    for (String date : range.dateKeys()) {
      counts.put(date + "|EXPEDITED", 0);
      counts.put(date + "|ASSESSMENT", 0);
      counts.put(date + "|DUE_CHANGE", 0);
    }
    successRows.forEach(row -> incrementCount(counts, row.getStatDate() + "|" + normalizeTaskType(row.getTaskType())));
    List<ReportDashboardVO.QuestionerTrendPoint> result = new ArrayList<>();
    for (String date : range.dateKeys()) {
      result.add(new ReportDashboardVO.QuestionerTrendPoint(date, "EXPEDITED", "订单加急任务", counts.getOrDefault(date + "|EXPEDITED", 0)));
      result.add(new ReportDashboardVO.QuestionerTrendPoint(date, "ASSESSMENT", "未下单咨询任务", counts.getOrDefault(date + "|ASSESSMENT", 0)));
      result.add(new ReportDashboardVO.QuestionerTrendPoint(date, "DUE_CHANGE", "客期变更任务", counts.getOrDefault(date + "|DUE_CHANGE", 0)));
    }
    return result;
  }

  private List<ReportDashboardVO.AgingTrendPoint> buildCompletionAgingTrend(DateRange range,
                                                                            List<ExpeditedTaskEntity> expeditedTasks,
                                                                            List<DeliveryAssessmentTaskEntity> assessmentTasks,
                                                                            List<CustomerDueChangeTaskEntity> dueChangeTasks) {
    List<ReportDashboardVO.AgingTrendPoint> result = new ArrayList<>();
    result.addAll(buildAgingSeries(range, "EXPEDITED", "订单加急任务", expeditedTasks.stream()
      .filter(task -> isCompleted(task.getOverallTaskStatus(), task.getTaskEvalStatus(), task.getCompletedAt()))
      .toList(), ExpeditedTaskEntity::getCompletedAt, ExpeditedTaskEntity::getCompletionDurationHours));
    result.addAll(buildAgingSeries(range, "ASSESSMENT", "未下单咨询任务", assessmentTasks.stream()
      .filter(task -> isCompleted(task.getOverallTaskStatus(), task.getTaskEvalStatus(), task.getCompletedAt()))
      .toList(), DeliveryAssessmentTaskEntity::getCompletedAt, DeliveryAssessmentTaskEntity::getCompletionDurationHours));
    result.addAll(buildAgingSeries(range, "DUE_CHANGE", "客期变更任务", dueChangeTasks.stream()
      .filter(task -> isCompleted(task.getOverallTaskStatus(), task.getTaskEvalStatus(), task.getCompletedAt()))
      .toList(), CustomerDueChangeTaskEntity::getCompletedAt, CustomerDueChangeTaskEntity::getCompletionDurationHours));
    return result;
  }

  private <T> List<ReportDashboardVO.AgingTrendPoint> buildAgingSeries(DateRange range,
                                                                       String taskType,
                                                                       String taskLabel,
                                                                       List<T> items,
                                                                       java.util.function.Function<T, String> completedAtGetter,
                                                                       java.util.function.Function<T, BigDecimal> durationGetter) {
    Map<String, List<Double>> bucketMap = new LinkedHashMap<>();
    range.dateKeys().forEach(date -> bucketMap.put(date, new ArrayList<>()));
    for (T item : items) {
      String date = toDateText(completedAtGetter.apply(item));
      if (bucketMap.containsKey(date)) {
        Double value = decimalToDouble(durationGetter.apply(item));
        if (value != null) {
          bucketMap.get(date).add(value);
        }
      }
    }
    return range.dateKeys().stream()
      .map(date -> new ReportDashboardVO.AgingTrendPoint(date, taskType, taskLabel, avg(bucketMap.get(date))))
      .toList();
  }

  private List<ReportDashboardVO.CoordinatorEfficiencyPoint> buildCoordinatorEfficiency(DateRange range,
                                                                                        List<ExpeditedTaskEntity> expeditedTasks,
                                                                                        List<DeliveryAssessmentTaskEntity> assessmentTasks,
                                                                                        List<CustomerDueChangeTaskEntity> dueChangeTasks) {
    Map<String, CoordinatorMetric> metrics = new LinkedHashMap<>();
    expeditedTasks.stream()
      .filter(task -> isCompleted(task.getOverallTaskStatus(), task.getTaskEvalStatus(), task.getCompletedAt()) && withinRange(task.getCompletedAt(), range))
      .forEach(task -> appendCoordinatorMetric(metrics, normalizeText(task.getProductCoordinator()), "订单加急任务", decimalToDouble(task.getCompletionDurationHours())));
    assessmentTasks.stream()
      .filter(task -> isCompleted(task.getOverallTaskStatus(), task.getTaskEvalStatus(), task.getCompletedAt()) && withinRange(task.getCompletedAt(), range))
      .forEach(task -> appendCoordinatorMetric(metrics, normalizeText(task.getProductCoordinator()), "未下单咨询任务", decimalToDouble(task.getCompletionDurationHours())));
    dueChangeTasks.stream()
      .filter(task -> isCompleted(task.getOverallTaskStatus(), task.getTaskEvalStatus(), task.getCompletedAt()) && withinRange(task.getCompletedAt(), range))
      .forEach(task -> appendCoordinatorMetric(metrics, normalizeText(task.getProductCoordinator()), "客期变更任务", decimalToDouble(task.getCompletionDurationHours())));
    return metrics.entrySet().stream()
      .filter(entry -> !isBlank(entry.getKey()))
      .map(entry -> new ReportDashboardVO.CoordinatorEfficiencyPoint(entry.getKey(), entry.getValue().taskLabels(), entry.getValue().count(), avg(entry.getValue().durations()), max(entry.getValue().durations())))
      .sorted(Comparator.comparing(ReportDashboardVO.CoordinatorEfficiencyPoint::completedCount).reversed())
      .limit(12)
      .toList();
  }

  private List<ReportDashboardVO.PendingOwnerPoint> buildRegionalPending(List<ExpeditedTaskEntity> expeditedTasks,
                                                                         List<CustomerDueChangeTaskEntity> dueChangeTasks) {
    Map<String, Integer> counts = new LinkedHashMap<>();
    expeditedTasks.stream()
      .filter(task -> isPendingToday(task.getCreatedAt(), task.getOverallTaskStatus(), task.getTaskEvalStatus(), task.getCompletedAt()))
      .forEach(task -> incrementCount(counts, normalizeText(task.getRegionalCoordinator())));
    dueChangeTasks.stream()
      .filter(task -> isPendingToday(task.getCreatedAt(), task.getOverallTaskStatus(), task.getTaskEvalStatus(), task.getCompletedAt()))
      .forEach(task -> incrementCount(counts, normalizeText(task.getRegionalCoordinator())));
    return counts.entrySet().stream()
      .filter(entry -> !isBlank(entry.getKey()))
      .map(entry -> new ReportDashboardVO.PendingOwnerPoint("regional", entry.getKey(), entry.getValue()))
      .sorted(Comparator.comparing(ReportDashboardVO.PendingOwnerPoint::pendingCount).reversed())
      .toList();
  }

  private List<ReportDashboardVO.PendingOwnerPoint> buildProductPending(List<ExpeditedTaskEntity> expeditedTasks,
                                                                        List<DeliveryAssessmentTaskEntity> assessmentTasks,
                                                                        List<CustomerDueChangeTaskEntity> dueChangeTasks) {
    Map<String, Integer> counts = new LinkedHashMap<>();
    expeditedTasks.stream()
      .filter(task -> isPendingToday(task.getCreatedAt(), task.getOverallTaskStatus(), task.getTaskEvalStatus(), task.getCompletedAt()))
      .forEach(task -> incrementCount(counts, normalizeText(task.getProductCoordinator())));
    assessmentTasks.stream()
      .filter(task -> isPendingToday(task.getCreatedAt(), task.getOverallTaskStatus(), task.getTaskEvalStatus(), task.getCompletedAt()))
      .forEach(task -> incrementCount(counts, normalizeText(task.getProductCoordinator())));
    dueChangeTasks.stream()
      .filter(task -> isPendingToday(task.getCreatedAt(), task.getOverallTaskStatus(), task.getTaskEvalStatus(), task.getCompletedAt()))
      .forEach(task -> incrementCount(counts, normalizeText(task.getProductCoordinator())));
    return counts.entrySet().stream()
      .filter(entry -> !isBlank(entry.getKey()))
      .map(entry -> new ReportDashboardVO.PendingOwnerPoint("product", entry.getKey(), entry.getValue()))
      .sorted(Comparator.comparing(ReportDashboardVO.PendingOwnerPoint::pendingCount).reversed())
      .toList();
  }

  private ReportDashboardVO.TaskOverview buildTaskOverview(String taskType, String taskLabel, List<?> tasks) {
    int submittedRows = tasks.size();
    int completedRows = 0;
    int unfinishedRows = 0;
    List<Double> completedHours = new ArrayList<>();
    for (Object task : tasks) {
      String overallStatus;
      String evalStatus;
      String completedAt;
      BigDecimal duration;
      if (task instanceof ExpeditedTaskEntity expeditedTask) {
        overallStatus = expeditedTask.getOverallTaskStatus();
        evalStatus = expeditedTask.getTaskEvalStatus();
        completedAt = expeditedTask.getCompletedAt();
        duration = expeditedTask.getCompletionDurationHours();
      } else if (task instanceof DeliveryAssessmentTaskEntity assessmentTask) {
        overallStatus = assessmentTask.getOverallTaskStatus();
        evalStatus = assessmentTask.getTaskEvalStatus();
        completedAt = assessmentTask.getCompletedAt();
        duration = assessmentTask.getCompletionDurationHours();
      } else if (task instanceof CustomerDueChangeTaskEntity dueChangeTask) {
        overallStatus = dueChangeTask.getOverallTaskStatus();
        evalStatus = dueChangeTask.getTaskEvalStatus();
        completedAt = dueChangeTask.getCompletedAt();
        duration = dueChangeTask.getCompletionDurationHours();
      } else {
        continue;
      }
      if (isCompleted(overallStatus, evalStatus, completedAt)) {
        completedRows++;
        Double value = decimalToDouble(duration);
        if (value != null) {
          completedHours.add(value);
        }
      } else {
        unfinishedRows++;
      }
    }
    return new ReportDashboardVO.TaskOverview(taskType, taskLabel, submittedRows, completedRows, unfinishedRows, avg(completedHours));
  }

  private List<ReportWorkbenchVO.PendingTaskVO> buildPendingTasks(String currentUserName,
                                                                 List<ExpeditedTaskEntity> expeditedTasks,
                                                                 List<DeliveryAssessmentTaskEntity> assessmentTasks,
                                                                 List<CustomerDueChangeTaskEntity> dueChangeTasks) {
    List<ReportWorkbenchVO.PendingTaskVO> result = new ArrayList<>();
    expeditedTasks.forEach(task -> {
      List<String> relatedRoles = resolveExpeditedRelatedRoles(task, currentUserName);
      if (relatedRoles.isEmpty() || !isPendingEvaluation(task.getTaskEvalStatus())) {
        return;
      }
      result.add(new ReportWorkbenchVO.PendingTaskVO(
        "EXPEDITED",
        "已下单加急任务",
        task.getId(),
        task.getTaskNo(),
        String.join("、", relatedRoles),
        firstNonBlank(task.getQuestionerName(), "-"),
        firstNonBlank(task.getProjectName(), "-"),
        task.getCreatedAt(),
        firstNonBlank(task.getTaskEvalStatus(), "未评审"),
        firstNonBlank(task.getOverallTaskStatus(), "未完成")
      ));
    });
    assessmentTasks.forEach(task -> {
      List<String> relatedRoles = resolveAssessmentRelatedRoles(task, currentUserName);
      if (relatedRoles.isEmpty() || !isPendingEvaluation(task.getTaskEvalStatus())) {
        return;
      }
      result.add(new ReportWorkbenchVO.PendingTaskVO(
        "ASSESSMENT",
        "未下单交期评估任务",
        task.getId(),
        task.getTaskNo(),
        String.join("、", relatedRoles),
        firstNonBlank(task.getQuestionerName(), "-"),
        firstNonBlank(task.getProjectName(), task.getCustomerName(), "-"),
        task.getCreatedAt(),
        firstNonBlank(task.getTaskEvalStatus(), "未评审"),
        firstNonBlank(task.getOverallTaskStatus(), "未完成")
      ));
    });
    dueChangeTasks.forEach(task -> {
      List<String> relatedRoles = resolveDueChangeRelatedRoles(task, currentUserName);
      if (relatedRoles.isEmpty() || !isPendingEvaluation(task.getTaskEvalStatus())) {
        return;
      }
      result.add(new ReportWorkbenchVO.PendingTaskVO(
        "DUE_CHANGE",
        "客期变更任务",
        task.getId(),
        task.getTaskNo(),
        String.join("、", relatedRoles),
        firstNonBlank(task.getQuestionerName(), "-"),
        firstNonBlank(task.getProjectName(), "-"),
        task.getCreatedAt(),
        firstNonBlank(task.getTaskEvalStatus(), "未评审"),
        firstNonBlank(task.getOverallTaskStatus(), "未完成")
      ));
    });
    result.sort(Comparator.comparing(ReportWorkbenchVO.PendingTaskVO::createdAt, Comparator.nullsLast(String::compareTo)).reversed());
    return result;
  }

  private List<ReportWorkbenchVO.UnreadConsultVO> buildUnreadConsults(String currentUserName, List<ManualConsultSessionEntity> sessions) {
    return sessions.stream()
      .map(session -> toUnreadConsult(currentUserName, session))
      .filter(item -> item != null && item.unreadCount() > 0)
      .sorted(Comparator.comparing(ReportWorkbenchVO.UnreadConsultVO::lastMessageAt, Comparator.nullsLast(String::compareTo)).reversed())
      .toList();
  }

  private List<ReportWorkbenchVO.TaskTrendPoint> buildTaskTrend(DateRange range,
                                                                String currentUserName,
                                                                List<ExpeditedTaskEntity> expeditedTasks,
                                                                List<DeliveryAssessmentTaskEntity> assessmentTasks,
                                                                List<CustomerDueChangeTaskEntity> dueChangeTasks) {
    Map<String, Integer> counts = new LinkedHashMap<>();
    for (String date : range.dateKeys()) {
      counts.put(date + "|EXPEDITED", 0);
      counts.put(date + "|ASSESSMENT", 0);
      counts.put(date + "|DUE_CHANGE", 0);
    }
    expeditedTasks.stream()
      .filter(task -> !resolveExpeditedRelatedRoles(task, currentUserName).isEmpty() && withinRange(task.getCreatedAt(), range))
      .forEach(task -> incrementCount(counts, toDateText(task.getCreatedAt()) + "|EXPEDITED"));
    assessmentTasks.stream()
      .filter(task -> !resolveAssessmentRelatedRoles(task, currentUserName).isEmpty() && withinRange(task.getCreatedAt(), range))
      .forEach(task -> incrementCount(counts, toDateText(task.getCreatedAt()) + "|ASSESSMENT"));
    dueChangeTasks.stream()
      .filter(task -> !resolveDueChangeRelatedRoles(task, currentUserName).isEmpty() && withinRange(task.getCreatedAt(), range))
      .forEach(task -> incrementCount(counts, toDateText(task.getCreatedAt()) + "|DUE_CHANGE"));

    List<ReportWorkbenchVO.TaskTrendPoint> result = new ArrayList<>();
    for (String date : range.dateKeys()) {
      result.add(new ReportWorkbenchVO.TaskTrendPoint(date, "EXPEDITED", "已下单加急任务", counts.getOrDefault(date + "|EXPEDITED", 0)));
      result.add(new ReportWorkbenchVO.TaskTrendPoint(date, "ASSESSMENT", "未下单交期评估任务", counts.getOrDefault(date + "|ASSESSMENT", 0)));
      result.add(new ReportWorkbenchVO.TaskTrendPoint(date, "DUE_CHANGE", "客期变更任务", counts.getOrDefault(date + "|DUE_CHANGE", 0)));
    }
    return result;
  }

  private List<ReportWorkbenchVO.ChatTrendPoint> buildChatTrend(DateRange range, String currentUserName, List<ManualConsultSessionEntity> sessions) {
    Map<String, Integer> counts = initBucketCounts(range);
    sessions.stream()
      .filter(session -> resolveConsultRelation(currentUserName, session).related())
      .map(session -> firstNonBlank(session.getLastMessageAt(), session.getCreatedAt()))
      .filter(dateTime -> withinRange(dateTime, range))
      .forEach(dateTime -> incrementCount(counts, toDateText(dateTime)));
    return range.dateKeys().stream()
      .map(date -> new ReportWorkbenchVO.ChatTrendPoint(date, counts.getOrDefault(date, 0)))
      .toList();
  }

  private boolean isPendingEvaluation(String taskEvalStatus) {
    String normalized = normalizeText(taskEvalStatus);
    return !isBlank(normalized)
      && (normalized.contains("未评审") || normalized.contains("待评估") || normalized.contains("未评估"));
  }

  private List<String> resolveExpeditedRelatedRoles(ExpeditedTaskEntity task, String currentUserName) {
    List<String> roles = new ArrayList<>();
    appendIfRelated(roles, "产品统筹", task.getProductCoordinator(), currentUserName);
    appendIfRelated(roles, "区域统筹", task.getRegionalCoordinator(), currentUserName);
    appendIfRelated(roles, "任务提交人", task.getQuestionerName(), currentUserName);
    appendIfRelated(roles, "协作人", task.getCollaborator(), currentUserName);
    appendIfRelated(roles, "商务助理", task.getBusinessAssistant(), currentUserName);
    appendIfRelated(roles, "业务员姓名", task.getSalespersonName(), currentUserName);
    appendIfRelated(roles, "代理区域统筹", task.getAgentRegionalCoordinator(), currentUserName);
    appendIfRelated(roles, "代理产品统筹", task.getAgentProductCoordinator(), currentUserName);
    return roles;
  }

  private List<String> resolveAssessmentRelatedRoles(DeliveryAssessmentTaskEntity task, String currentUserName) {
    List<String> roles = new ArrayList<>();
    appendIfRelated(roles, "产品统筹", task.getProductCoordinator(), currentUserName);
    appendIfRelated(roles, "代理产品统筹", task.getAgentProductCoordinator(), currentUserName);
    appendIfRelated(roles, "提问人", task.getQuestionerName(), currentUserName);
    appendIfRelated(roles, "协作人", task.getCollaborator(), currentUserName);
    return roles;
  }

  private List<String> resolveDueChangeRelatedRoles(CustomerDueChangeTaskEntity task, String currentUserName) {
    List<String> roles = new ArrayList<>();
    appendIfRelated(roles, "产品统筹", task.getProductCoordinator(), currentUserName);
    appendIfRelated(roles, "区域统筹", task.getRegionalCoordinator(), currentUserName);
    appendIfRelated(roles, "任务提交人", task.getQuestionerName(), currentUserName);
    appendIfRelated(roles, "业务员姓名", task.getSalespersonName(), currentUserName);
    appendIfRelated(roles, "商务助理", task.getBusinessAssistant(), currentUserName);
    appendIfRelated(roles, "协作人", task.getCollaborator(), currentUserName);
    appendIfRelated(roles, "代理区域统筹", task.getAgentRegionalCoordinator(), currentUserName);
    appendIfRelated(roles, "代理产品统筹", task.getAgentProductCoordinator(), currentUserName);
    return roles;
  }

  private ReportWorkbenchVO.UnreadConsultVO toUnreadConsult(String currentUserName, ManualConsultSessionEntity session) {
    ConsultRelation relation = resolveConsultRelation(currentUserName, session);
    if (!relation.related() || relation.unreadCount() <= 0) {
      return null;
    }
    return new ReportWorkbenchVO.UnreadConsultVO(
      session.getId(),
      firstNonBlank(session.getSessionNo(), "-"),
      firstNonBlank(session.getTitle(), "人工咨询"),
      relation.counterpartName(),
      firstNonBlank(session.getLastMessagePreview(), "-"),
      firstNonBlank(session.getLastMessageAt(), session.getCreatedAt()),
      relation.unreadCount()
    );
  }

  private void appendIfRelated(List<String> roles, String roleLabel, String fieldValue, String currentUserName) {
    if (matchesUser(currentUserName, fieldValue)) {
      roles.add(roleLabel);
    }
  }

  private boolean matchesUser(String currentUserName, String fieldValue) {
    String normalizedUserName = normalizeText(currentUserName);
    String normalizedFieldValue = normalizeText(fieldValue);
    if (isBlank(normalizedUserName) || isBlank(normalizedFieldValue)) {
      return false;
    }
    if (normalizedUserName.equalsIgnoreCase(normalizedFieldValue)) {
      return true;
    }
    for (String segment : normalizedFieldValue.split("[,，、/；;\\s]+")) {
      if (normalizedUserName.equalsIgnoreCase(normalizeText(segment))) {
        return true;
      }
    }
    return false;
  }

  private ConsultRelation resolveConsultRelation(String currentUserName, ManualConsultSessionEntity session) {
    boolean isQuestioner = matchesUser(currentUserName, session.getQuestionerName());
    boolean isService = matchesUser(currentUserName, session.getServiceDisplayName())
      || matchesUser(currentUserName, session.getServiceAlias())
      || matchesUser(currentUserName, session.getTransferredFromName());
    if (!isQuestioner && !isService) {
      return new ConsultRelation(false, 0, "");
    }
    if (isQuestioner) {
      return new ConsultRelation(
        true,
        session.getUnreadForQuestioner() == null ? 0 : session.getUnreadForQuestioner(),
        firstNonBlank(session.getServiceAlias(), session.getServiceDisplayName(), "人工客服")
      );
    }
    return new ConsultRelation(
      true,
      session.getUnreadForService() == null ? 0 : session.getUnreadForService(),
      firstNonBlank(session.getQuestionerName(), "咨询人")
    );
  }

  private TaskQueryDTO emptyQuery() {
    return new TaskQueryDTO(null, null, null, null, null, null, null, null, null, null, null);
  }

  private DateRange resolveRange(String range) {
    String normalized = normalizeRange(range);
    LocalDate end = LocalDate.now();
    LocalDate start = switch (normalized) {
      case "today" -> end;
      case "month" -> end.minusDays(29);
      default -> end.minusDays(6);
    };
    List<String> dateKeys = new ArrayList<>();
    LocalDate cursor = start;
    while (!cursor.isAfter(end)) {
      dateKeys.add(cursor.format(DATE_FORMATTER));
      cursor = cursor.plusDays(1);
    }
    return new DateRange(normalized, start.format(DATE_FORMATTER), end.format(DATE_FORMATTER), dateKeys);
  }

  private boolean withinRange(String dateTimeText, DateRange range) {
    String date = toDateText(dateTimeText);
    return !isBlank(date) && date.compareTo(range.startDate()) >= 0 && date.compareTo(range.endDate()) <= 0;
  }

  private boolean isPendingToday(String createdAt, String overallStatus, String evalStatus, String completedAt) {
    return LocalDate.now().format(DATE_FORMATTER).equals(toDateText(createdAt))
      && !isCompleted(overallStatus, evalStatus, completedAt);
  }

  private boolean isCompleted(String overallStatus, String evalStatus, String completedAt) {
    if (!isBlank(completedAt)) {
      return true;
    }
    String normalizedOverall = normalizeText(overallStatus);
    String normalizedEval = normalizeText(evalStatus);
    return normalizedOverall.contains("已完成")
      || normalizedEval.contains("已完成")
      || normalizedEval.contains("完成")
      || normalizedEval.contains("闭环")
      || (!isBlank(normalizedEval) && !normalizedEval.contains("未评估") && !normalizedEval.contains("待评估") && !normalizedEval.contains("处理中") && !normalizedEval.contains("进行中"));
  }

  private String toDateText(String dateTimeText) {
    if (isBlank(dateTimeText)) {
      return "";
    }
    return dateTimeText.trim().length() >= 10 ? dateTimeText.trim().substring(0, 10) : dateTimeText.trim();
  }

  private Map<String, Integer> initBucketCounts(DateRange range) {
    Map<String, Integer> result = new LinkedHashMap<>();
    range.dateKeys().forEach(date -> result.put(date, 0));
    return result;
  }

  private void incrementCount(Map<String, Integer> counts, String key) {
    if (isBlank(key)) {
      return;
    }
    counts.put(key, counts.getOrDefault(key, 0) + 1);
  }

  private void appendCoordinatorMetric(Map<String, CoordinatorMetric> metrics, String owner, String taskLabel, Double duration) {
    if (isBlank(owner)) {
      return;
    }
    CoordinatorMetric metric = metrics.computeIfAbsent(owner, key -> new CoordinatorMetric());
    metric.taskLabelSet.add(taskLabel);
    if (duration != null) {
      metric.durations.add(duration);
    }
    metric.count++;
  }

  private double avg(List<Double> values) {
    if (values == null || values.isEmpty()) {
      return 0D;
    }
    double sum = 0D;
    for (Double value : values) {
      sum += value;
    }
    return round(sum / values.size());
  }

  private double max(List<Double> values) {
    if (values == null || values.isEmpty()) {
      return 0D;
    }
    double result = 0D;
    for (Double value : values) {
      result = Math.max(result, value == null ? 0D : value);
    }
    return round(result);
  }

  private double round(double value) {
    return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
  }

  private Double decimalToDouble(BigDecimal value) {
    return value == null ? null : value.doubleValue();
  }

  private String normalizeText(String value) {
    return value == null ? "" : value.trim();
  }

  private String normalizeRange(String value) {
    String normalized = normalizeText(value).toLowerCase(Locale.ROOT);
    if (!List.of("today", "week", "month").contains(normalized)) {
      return "week";
    }
    return normalized;
  }

  private String normalizeTaskType(String value) {
    String normalized = normalizeText(value).toUpperCase(Locale.ROOT);
    return switch (normalized) {
      case "EXPEDITED", "ASSESSMENT", "DUE_CHANGE" -> normalized;
      default -> "ASSESSMENT";
    };
  }

  private boolean isBlank(String value) {
    return value == null || value.trim().isEmpty();
  }

  private String firstNonBlank(String... values) {
    for (String value : values) {
      if (!isBlank(value)) {
        return value.trim();
      }
    }
    return "";
  }

  private List<SectionSetting> readSections(String json) {
    if (isBlank(json)) {
      return DEFAULT_SECTIONS;
    }
    try {
      List<SectionSetting> parsed = objectMapper.readValue(json, new TypeReference<List<SectionSetting>>() {
      });
      return parsed == null || parsed.isEmpty() ? DEFAULT_SECTIONS : parsed;
    } catch (JsonProcessingException exception) {
      return DEFAULT_SECTIONS;
    }
  }

  private List<SectionSetting> mergeSections(List<ReportDashboardConfigUpdateDTO.ReportDashboardSectionDTO> sections) {
    Map<String, SectionSetting> current = new LinkedHashMap<>();
    DEFAULT_SECTIONS.forEach(section -> current.put(section.key(), section));
    if (sections != null) {
      for (ReportDashboardConfigUpdateDTO.ReportDashboardSectionDTO section : sections) {
        if (section == null || isBlank(section.key()) || !current.containsKey(section.key().trim())) {
          continue;
        }
        current.put(section.key().trim(), new SectionSetting(
          section.key().trim(),
          firstNonBlank(section.label(), current.get(section.key().trim()).label()),
          section.sort() == null ? current.get(section.key().trim()).sort() : section.sort(),
          section.enabled() == null || section.enabled() != 0
        ));
      }
    }
    return current.values().stream().sorted(Comparator.comparing(SectionSetting::sort)).toList();
  }

  private String writeSections(List<SectionSetting> sections) {
    try {
      return objectMapper.writeValueAsString(sections);
    } catch (JsonProcessingException exception) {
      throw new BusinessException(500, "报表配置序列化失败");
    }
  }

  private record DateRange(String rangeKey, String startDate, String endDate, List<String> dateKeys) {
  }

  private record ConsultRelation(boolean related, int unreadCount, String counterpartName) {
  }

  private record SectionSetting(String key, String label, Integer sort, boolean enabled) {
  }

  private static class CoordinatorMetric {
    private int count;
    private final List<Double> durations = new ArrayList<>();
    private final LinkedHashSet<String> taskLabelSet = new LinkedHashSet<>();

    public int count() {
      return count;
    }

    public List<Double> durations() {
      return durations;
    }

    public String taskLabels() {
      return String.join("、", taskLabelSet);
    }
  }
}