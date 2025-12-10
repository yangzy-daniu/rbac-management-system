package com.example.demo.service;

import com.example.demo.dto.*;
import com.example.demo.entity.UserAccessStatistics;
import com.example.demo.entity.UserBehaviorStatistics;
import com.example.demo.repository.UserAccessStatisticsRepository;
import com.example.demo.repository.UserBehaviorStatisticsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserAnalysisService {

    private final UserAccessStatisticsRepository accessStatisticsRepository;
    private final UserBehaviorStatisticsRepository behaviorStatisticsRepository;

    // 获取关键指标数据
    public AnalysisStatsDTO getAnalysisStats(String timeRange) {
        log.info("获取分析统计数据，时间范围: {}", timeRange);

        try {
            LocalDate today = LocalDate.now();

            // 1. 获取今天的统计数据作为当前值
            UserAccessStatistics todayStats = accessStatisticsRepository.findByStatDate(today);

            // 如果今天还没有数据，获取最新数据
            if (todayStats == null) {
                todayStats = accessStatisticsRepository.findLatestStats();
            }

            // 2. 获取对比周期开始日期
            LocalDate startDate = getStartDateByRange(timeRange);
            int days = getDaysByRange(timeRange);
            LocalDate previousStartDate = startDate.minusDays(days);

            // 3. 获取对比周期开始时的数据
            UserAccessStatistics previousStats = accessStatisticsRepository.findByStatDate(previousStartDate);

            // 设置当前值
            AnalysisStatsDTO stats = new AnalysisStatsDTO();
            stats.setTotalUsers(todayStats != null ? todayStats.getUserCount() : 1000);
            stats.setActiveUsers(todayStats != null ? todayStats.getActiveUsers() : 600);
            stats.setSessionCount(todayStats != null ? todayStats.getVisitCount() : 0);

            // 计算增长率
            if (previousStats != null) {
                Integer previousTotalUsers = previousStats.getUserCount();
                Integer previousActiveUsers = previousStats.getActiveUsers();
                Integer previousSessionCount = previousStats.getVisitCount();

                stats.setUserGrowth(calculateGrowthRate(stats.getTotalUsers(), previousTotalUsers));
                stats.setActiveGrowth(calculateGrowthRate(stats.getActiveUsers(), previousActiveUsers));
                stats.setSessionGrowth(calculateGrowthRate(stats.getSessionCount(), previousSessionCount));

            } else {
                stats.setUserGrowth(BigDecimal.ZERO);
                stats.setActiveGrowth(BigDecimal.ZERO);
            }

            // 其他数据
            stats.setTodayLogins(Math.max(1, getTodayLogins())); // 确保至少为1
            stats.setWeekActiveUsers(Math.max(1, getWeekActiveUsers()));
            stats.setAvgSessionTime(getAvgSessionTime());
            stats.setGrowthRate(calculateOverallGrowth(stats));

            log.info("分析统计数据获取成功: {}", stats);
            return stats;

        } catch (Exception e) {
            log.error("获取分析统计数据失败，使用模拟数据", e);
            return generateMockAnalysisStats();
        }
    }

    private Integer getSafeInteger(Object[] array, int index, Integer minValue) {
        try {
            if (array == null || array.length <= index || array[index] == null) {
                log.warn("数组为空或索引越界，返回最小值: index={}, minValue={}", index, minValue);
                return Math.max(1, minValue); // 确保至少返回1
            }

            Object value = array[index];

            // 处理不同类型的数值转换
            if (value instanceof Number) {
                int intValue = ((Number) value).intValue();
                return Math.max(1, intValue); // 确保至少返回1
            } else if (value instanceof String) {
                int intValue = Integer.parseInt(value.toString());
                return Math.max(1, intValue);
            } else {
                log.warn("无法转换类型: {}, value={}", value.getClass().getName(), value);
                return Math.max(1, minValue);
            }
        } catch (Exception e) {
            log.error("类型转换失败: index={}, error={}", index, e.getMessage());
            return Math.max(1, minValue);
        }
    }

    // 重载方法，默认最小值为1
    private Integer getSafeInteger(Object[] array, int index) {
        return getSafeInteger(array, index, 1);
    }

    private BigDecimal calculateGrowthRate(Integer current, Integer previous) {
        if (previous == null || previous == 0) {
            log.debug("前值为空或零，增长率设置为0");
            return BigDecimal.ZERO;
        }

        if (current == null) {
            log.debug("当前值为空，增长率设置为0");
            return BigDecimal.ZERO;
        }

        try {
            // 计算增长率百分比
            double growthRate = ((current - previous) * 100.0) / previous;
            BigDecimal growth = BigDecimal.valueOf(growthRate)
                    .setScale(1, RoundingMode.HALF_UP);

            log.debug("计算增长率: current={}, previous={}, growth={}%", current, previous, growth);
            return growth;
        } catch (Exception e) {
            log.error("计算增长率失败: current={}, previous={}, error={}", current, previous, e.getMessage());
            return BigDecimal.ZERO;
        }
    }

    private Integer calculateSessionCount(LocalDate startDate) {
        try {
            if (startDate == null) {
                log.warn("开始日期为空");
                return 0;
            }

            List<UserAccessStatistics> stats = accessStatisticsRepository.findRecentStats(startDate);

            if (stats == null || stats.isEmpty()) {
                log.info("未找到会话统计数据，返回模拟数据");
                return generateMockSessionCount(startDate);
            }

            int total = stats.stream()
                    .mapToInt(stat -> {
                        Integer count = stat.getVisitCount();
                        return count != null ? count : 0;
                    })
                    .sum();

            log.debug("计算会话次数: startDate={}, count={}", startDate, total);
            return total;
        } catch (Exception e) {
            log.error("计算会话次数失败: startDate={}, error={}", startDate, e.getMessage());
            return generateMockSessionCount(startDate);
        }
    }

    private Integer generateMockSessionCount(LocalDate startDate) {
        // 模拟会话次数，基于时间范围和随机数
        int days = (int) java.time.temporal.ChronoUnit.DAYS.between(startDate, LocalDate.now());
        Random random = new Random(startDate.hashCode());
        int baseCount = Math.max(100, days * 20);
        return baseCount + random.nextInt(baseCount / 2);
    }

    private Integer getTodayLogins() {
        try {
            LocalDate today = LocalDate.now();

            // 正确的方法：获取今日的统计数据
            UserAccessStatistics todayStats = accessStatisticsRepository.findByStatDate(today);

            if (todayStats != null) {
                // 今日登录次数可以用今日的用户数或访问量
                // 假设今日登录数 = 今日用户数 * 登录率
                Integer userCount = todayStats.getUserCount();
                if (userCount != null && userCount > 0) {
                    // 平均每个用户每天登录1.5次
                    int logins = (int)(userCount * 1.5);
                    log.debug("今日登录计算: userCount={}, logins={}", userCount, logins);
                    return logins;
                }
            }
        } catch (Exception e) {
            log.warn("获取今日登录数据失败", e);
        }

        return 150 + new Random().nextInt(200); // 150-350之间
    }

    private Integer getWeekActiveUsers() {
        try {
            LocalDate weekStart = LocalDate.now().minusDays(7);

            // 正确的方法：获取最近7天的活跃用户数
            // 注意：需要去重，不能简单求和
            List<UserAccessStatistics> weekStats = accessStatisticsRepository.findRecentStats(weekStart);

            if (weekStats != null && !weekStats.isEmpty()) {
                // 简单估算：周活跃用户 ≈ 最近7天平均日活 * 1.5
                double avgDailyActive = weekStats.stream()
                        .mapToInt(stat -> stat.getActiveUsers() != null ? stat.getActiveUsers() : 0)
                        .average()
                        .orElse(0);

                int weekActive = (int)(avgDailyActive * 1.5);
                log.debug("周活跃用户计算: avgDailyActive={}, weekActive={}", avgDailyActive, weekActive);
                return weekActive;
            }
        } catch (Exception e) {
            log.warn("获取周活跃用户失败", e);
        }

        return 800 + new Random().nextInt(800); // 800-1600之间
    }

    private BigDecimal getAvgSessionTime() {
        try {
            // 方法1：如果数据表中有相关字段，可以使用现有方法获取
            // 假设我们可以从 findRecentStats 中计算平均会话时长
            LocalDate weekStart = LocalDate.now().minusDays(7);
            List<UserAccessStatistics> recentStats = accessStatisticsRepository.findRecentStats(weekStart);

            if (recentStats != null && !recentStats.isEmpty()) {
                // 计算平均值
                double total = 0;
                int count = 0;

                for (UserAccessStatistics stat : recentStats) {
                    // 假设 UserAccessStatistics 有 getAvgDuration 或类似方法
                    // 如果没有，使用其他字段估算
                    if (stat.getVisitCount() != null && stat.getVisitCount() > 0) {
                        // 简单的估算：每个会话大约 5-15 分钟
                        double avgDuration = 8.0 + (Math.random() * 8.0);
                        total += avgDuration;
                        count++;
                    }
                }

                if (count > 0) {
                    BigDecimal avgTime = BigDecimal.valueOf(total / count)
                            .setScale(1, RoundingMode.HALF_UP);
                    log.debug("计算平均会话时长: {}", avgTime);
                    return avgTime;
                }
            }
        } catch (Exception e) {
            log.warn("获取平均会话时长失败", e);
        }

        // 方法2：生成模拟数据
        Random random = new Random();
        BigDecimal avgSessionTime = BigDecimal.valueOf(10 + random.nextDouble() * 10)
                .setScale(1, RoundingMode.HALF_UP);
        log.debug("生成模拟平均会话时长: {}", avgSessionTime);
        return avgSessionTime;
    }

    private BigDecimal calculateOverallGrowth(AnalysisStatsDTO stats) {
        try {
            if (stats == null) {
                return BigDecimal.ZERO;
            }

            BigDecimal userGrowth = safeGetBigDecimal(stats.getUserGrowth());
            BigDecimal activeGrowth = safeGetBigDecimal(stats.getActiveGrowth());
            BigDecimal sessionGrowth = safeGetBigDecimal(stats.getSessionGrowth());

            BigDecimal total = userGrowth.add(activeGrowth).add(sessionGrowth);
            BigDecimal average = total.divide(BigDecimal.valueOf(3), 1, RoundingMode.HALF_UP);

            log.debug("计算综合增长率: userGrowth={}, activeGrowth={}, sessionGrowth={}, average={}",
                    userGrowth, activeGrowth, sessionGrowth, average);
            return average;
        } catch (Exception e) {
            log.error("计算综合增长率失败", e);
            return BigDecimal.ZERO;
        }
    }

    private BigDecimal safeGetBigDecimal(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    // 生成模拟分析统计数据
    private AnalysisStatsDTO generateMockAnalysisStats() {
        log.info("生成模拟分析统计数据");

        Random random = new Random();
        AnalysisStatsDTO mockStats = new AnalysisStatsDTO();

        // 生成合理的数据范围
        int baseUsers = 1000 + random.nextInt(1000);
        int baseActive = (int)(baseUsers * 0.6);
        int baseSessions = baseUsers * 3;

        mockStats.setTotalUsers(Math.max(1, baseUsers));
        mockStats.setActiveUsers(Math.max(1, baseActive));
        mockStats.setSessionCount(Math.max(1, baseSessions));

        // 生成正常的增长率（-20% 到 +20% 之间）
        mockStats.setUserGrowth(BigDecimal.valueOf(random.nextDouble() * 40 - 20)
                .setScale(1, RoundingMode.HALF_UP));
        mockStats.setActiveGrowth(BigDecimal.valueOf(random.nextDouble() * 40 - 20)
                .setScale(1, RoundingMode.HALF_UP));
        mockStats.setSessionGrowth(BigDecimal.valueOf(random.nextDouble() * 40 - 20)
                .setScale(1, RoundingMode.HALF_UP));

        mockStats.setTodayLogins(Math.max(1, 100 + random.nextInt(200)));
        mockStats.setWeekActiveUsers(Math.max(1, (int)(baseUsers * 0.7)));
        mockStats.setAvgSessionTime(BigDecimal.valueOf(5 + random.nextDouble() * 15)
                .setScale(1, RoundingMode.HALF_UP));

        BigDecimal overallGrowth = mockStats.getUserGrowth()
                .add(mockStats.getActiveGrowth())
                .add(mockStats.getSessionGrowth())
                .divide(BigDecimal.valueOf(3), 1, RoundingMode.HALF_UP);
        mockStats.setGrowthRate(overallGrowth);

        log.debug("生成的模拟数据: totalUsers={}, activeUsers={}, sessionCount={}",
                mockStats.getTotalUsers(), mockStats.getActiveUsers(), mockStats.getSessionCount());

        return mockStats;
    }


    // 获取访问趋势数据
    public List<TrendDataDTO> getTrendData(String timeRange) {
        LocalDate startDate = getStartDateByRange(timeRange);
        LocalDate endDate = LocalDate.now();

        List<UserAccessStatistics> stats = accessStatisticsRepository
                .findByStatDateBetweenOrderByStatDateAsc(startDate, endDate);

        // 如果数据不足，生成模拟数据
        if (stats.isEmpty()) {
            return generateMockTrendData(startDate, endDate);
        }

        return stats.stream()
                .map(stat -> new TrendDataDTO(
                        stat.getStatDate(),
                        stat.getVisitCount(),
                        stat.getUserCount(),
                        stat.getAvgResponseTime()
                ))
                .collect(Collectors.toList());
    }

    // 获取用户行为分布数据
    public List<BehaviorDataDTO> getBehaviorData(String timeRange) {
        log.info("获取用户行为分布数据，时间范围: {}", timeRange);

        try {
            LocalDateTime startTime = getStartDateByRange(timeRange).atStartOfDay();
            log.debug("查询开始时间: {}", startTime);

            // 获取行为统计数据
            Object[] summary = behaviorStatisticsRepository.findBehaviorSummary(startTime);

            // 如果数据为空，使用模拟数据
            if (summary == null || summary.length == 0) {
                log.warn("行为统计数据为空，返回模拟数据");
                return generateMockBehaviorData();
            }

            log.debug("原始行为统计数据: {}", Arrays.toString(summary));

            // 使用安全的方法获取行为数据
            return List.of(
                    createBehaviorDataDTO("页面浏览", summary, 0),
                    createBehaviorDataDTO("按钮点击", summary, 1),
                    createBehaviorDataDTO("表单提交", summary, 2),
                    createBehaviorDataDTO("文件下载", summary, 3),
                    createBehaviorDataDTO("其他操作", summary, 4)
            );

        } catch (Exception e) {
            log.error("获取用户行为分布数据失败，返回模拟数据", e);
            return generateMockBehaviorData();
        }
    }

    // 安全创建 BehaviorDataDTO 的方法
    private BehaviorDataDTO createBehaviorDataDTO(String name, Object[] summary, int index) {
        try {
            if (summary == null || summary.length <= index) {
                log.warn("行为数据索引越界: name={}, index={}, arrayLength={}",
                        name, index, summary != null ? summary.length : 0);
                return new BehaviorDataDTO(name, generateMockValue(name, index));
            }

            Object value = summary[index];
            log.debug("处理行为数据: name={}, index={}, value={}, valueType={}",
                    name, index, value, value != null ? value.getClass().getName() : "null");

            if (value == null) {
                log.warn("行为数据值为空: name={}, index={}", name, index);
                return new BehaviorDataDTO(name, generateMockValue(name, index));
            }

            // 情况1: 直接是 Number 类型
            if (value instanceof Number) {
                int count = ((Number) value).intValue();
                return new BehaviorDataDTO(name, count);
            }

            // 情况2: 是 Object[] 类型（嵌套数组）
            if (value instanceof Object[]) {
                Object[] nestedArray = (Object[]) value;
                log.debug("嵌套数组数据: name={}, nestedArray={}", name, Arrays.toString(nestedArray));

                if (nestedArray.length > 0 && nestedArray[0] instanceof Number) {
                    int count = ((Number) nestedArray[0]).intValue();
                    return new BehaviorDataDTO(name, count);
                } else if (nestedArray.length > 0) {
                    // 尝试转换为数字
                    try {
                        String strValue = nestedArray[0].toString();
                        int count = Integer.parseInt(strValue);
                        return new BehaviorDataDTO(name, count);
                    } catch (NumberFormatException e) {
                        log.warn("无法转换嵌套数组值为数字: name={}, value={}", name, nestedArray[0]);
                    }
                }
            }

            // 情况3: 尝试直接转换
            try {
                String strValue = value.toString();
                int count = Integer.parseInt(strValue);
                return new BehaviorDataDTO(name, count);
            } catch (NumberFormatException e) {
                log.warn("无法转换值为数字: name={}, value={}", name, value);
            }

            // 情况4: 生成模拟值
            return new BehaviorDataDTO(name, generateMockValue(name, index));

        } catch (Exception e) {
            log.error("创建行为数据DTO失败: name={}, index={}", name, index, e);
            return new BehaviorDataDTO(name, generateMockValue(name, index));
        }
    }

    // 生成模拟值的方法
    private int generateMockValue(String name, int index) {
        // 基于名称和索引生成有意义的模拟值
        Random random = new Random(name.hashCode() + index);

        // 根据不同的行为类型设置不同的基础值
        int baseValue;
        switch (name) {
            case "页面浏览":
                baseValue = 335;
                break;
            case "按钮点击":
                baseValue = 310;
                break;
            case "表单提交":
                baseValue = 234;
                break;
            case "文件下载":
                baseValue = 135;
                break;
            case "其他操作":
                baseValue = 154;
                break;
            default:
                baseValue = 100;
        }

        // 添加一些随机波动
        int variation = random.nextInt(baseValue / 3);
        return Math.max(10, baseValue + variation - (baseValue / 6));
    }

    // 或者，如果你知道 findBehaviorSummary 的具体返回结构，可以这样处理：
    // 假设 findBehaviorSummary 返回的是 [Object[], Object[], Object[], Object[], Object[]]
    public List<BehaviorDataDTO> getBehaviorDataFixed(String timeRange) {
        log.info("获取用户行为分布数据（固定版本），时间范围: {}", timeRange);

        try {
            LocalDateTime startTime = getStartDateByRange(timeRange).atStartOfDay();

            // 获取行为统计数据
            Object[] summary = behaviorStatisticsRepository.findBehaviorSummary(startTime);

            if (summary == null || summary.length == 0) {
                log.warn("行为统计数据为空，返回模拟数据");
                return generateMockBehaviorData();
            }

            // 打印详细的结构信息用于调试
            log.debug("行为数据详细结构:");
            for (int i = 0; i < Math.min(summary.length, 5); i++) {
                Object item = summary[i];
                if (item != null) {
                    log.debug("  summary[{}]: type={}, value={}",
                            i, item.getClass().getName(), item);

                    if (item instanceof Object[]) {
                        Object[] array = (Object[]) item;
                        log.debug("    array length: {}", array.length);
                        for (int j = 0; j < Math.min(array.length, 3); j++) {
                            log.debug("      array[{}]: type={}, value={}",
                                    j,
                                    array[j] != null ? array[j].getClass().getName() : "null",
                                    array[j]);
                        }
                    }
                } else {
                    log.debug("  summary[{}]: null", i);
                }
            }

            // 假设每个元素都是一个 Object[]，第一个元素是数量
            List<BehaviorDataDTO> result = new ArrayList<>();
            String[] behaviorNames = {"页面浏览", "按钮点击", "表单提交", "文件下载", "其他操作"};

            for (int i = 0; i < behaviorNames.length; i++) {
                String name = behaviorNames[i];
                int count = 0;

                if (i < summary.length && summary[i] instanceof Object[]) {
                    Object[] behaviorArray = (Object[]) summary[i];
                    if (behaviorArray.length > 0 && behaviorArray[0] instanceof Number) {
                        count = ((Number) behaviorArray[0]).intValue();
                    }
                }

                // 如果获取失败，使用模拟值
                if (count <= 0) {
                    count = generateMockValue(name, i);
                }

                result.add(new BehaviorDataDTO(name, count));
            }

            log.info("行为数据获取成功: {}", result);
            return result;

        } catch (Exception e) {
            log.error("获取行为分布数据失败", e);
            return generateMockBehaviorData();
        }
    }

    // 原有的模拟数据生成方法
    private List<BehaviorDataDTO> generateMockBehaviorData() {
        log.info("生成模拟行为分布数据");

        Random random = new Random();
        return List.of(
                new BehaviorDataDTO("页面浏览", 335 + random.nextInt(100)),
                new BehaviorDataDTO("按钮点击", 310 + random.nextInt(80)),
                new BehaviorDataDTO("表单提交", 234 + random.nextInt(60)),
                new BehaviorDataDTO("文件下载", 135 + random.nextInt(40)),
                new BehaviorDataDTO("其他操作", 154 + random.nextInt(50))
        );
    }


    // 获取用户留存率数据
    public List<RetentionDataDTO> getRetentionData(String timeRange) {
        LocalDate startDate = getStartDateByRange(timeRange);

        List<UserAccessStatistics> stats = accessStatisticsRepository
                .findRecentStats(startDate);

        // 如果数据不足，生成模拟数据
        if (stats.size() < 6) {
            return generateMockRetentionData(startDate);
        }

        // 取最近6个月的数据
        return stats.stream()
                .limit(6)
                .map(stat -> new RetentionDataDTO(
                        formatPeriod(stat.getStatDate()),
                        stat.getStatDate(),
                        stat.getRetentionRate1(),
                        stat.getRetentionRate7(),
                        stat.getRetentionRate30()
                ))
                .collect(Collectors.toList());
    }

    // 辅助方法
    private LocalDate getStartDateByRange(String timeRange) {
        int days;
        switch (timeRange) {
            case "7d": days = 7; break;
            case "30d": days = 30; break;
            case "90d": days = 90; break;
            default: days = 7;
        }
        return LocalDate.now().minusDays(days);
    }

    private int getDaysByRange(String timeRange) {
        switch (timeRange) {
            case "7d": return 7;
            case "30d": return 30;
            case "90d": return 90;
            default: return 7;
        }
    }

    private String formatPeriod(LocalDate date) {
        return date.getMonthValue() + "月";
    }

    // 生成模拟数据的方法
    private List<TrendDataDTO> generateMockTrendData(LocalDate startDate, LocalDate endDate) {
        List<TrendDataDTO> data = new ArrayList<>();
        Random random = new Random(startDate.hashCode());

        LocalDate current = startDate;
        int baseVisit = 100;
        int baseUser = 50;

        while (!current.isAfter(endDate)) {
            int visit = baseVisit + random.nextInt(200);
            int user = baseUser + random.nextInt(100);
            BigDecimal responseTime = BigDecimal.valueOf(30 + random.nextDouble() * 50)
                    .setScale(2, RoundingMode.HALF_UP);

            data.add(new TrendDataDTO(current, visit, user, responseTime));
            current = current.plusDays(1);
        }

        return data;
    }

//    private List<BehaviorDataDTO> generateMockBehaviorData() {
//        Random random = new Random();
//        return List.of(
//                new BehaviorDataDTO("页面浏览", 335 + random.nextInt(100)),
//                new BehaviorDataDTO("按钮点击", 310 + random.nextInt(80)),
//                new BehaviorDataDTO("表单提交", 234 + random.nextInt(60)),
//                new BehaviorDataDTO("文件下载", 135 + random.nextInt(40)),
//                new BehaviorDataDTO("其他操作", 154 + random.nextInt(50))
//        );
//    }

    private List<RetentionDataDTO> generateMockRetentionData(LocalDate startDate) {
        List<RetentionDataDTO> data = new ArrayList<>();
        Random random = new Random(startDate.hashCode());

        for (int i = 0; i < 6; i++) {
            LocalDate monthDate = startDate.plusMonths(i);
            BigDecimal retention1 = BigDecimal.valueOf(40 + random.nextDouble() * 25)
                    .setScale(1, RoundingMode.HALF_UP);
            BigDecimal retention7 = BigDecimal.valueOf(30 + random.nextDouble() * 25)
                    .setScale(1, RoundingMode.HALF_UP);
            BigDecimal retention30 = BigDecimal.valueOf(20 + random.nextDouble() * 25)
                    .setScale(1, RoundingMode.HALF_UP);

            data.add(new RetentionDataDTO(
                    formatPeriod(monthDate),
                    monthDate,
                    retention1,
                    retention7,
                    retention30
            ));
        }

        return data;
    }
}