package com.demo.inter3idemo;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import lombok.Builder;
import lombok.Data;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author erwinfu
 * TODO 待补充执行日志和运维周期、
 * @description 设备任务查询
 * @date 2023/6/26 14:12
 */
@Data
public class QueryEquipmentTaskApplicationService {
    /**
     * 设备列表
     */
    private List<EquipmentDO> equipmentDOS = new ArrayList<>();
    /**
     * 任务列表
     */
    private List<TaskDO> taskDOS = new ArrayList<>();
    /**
     * 设备任务调度记录
     */
    private List<EquipmentTaskDispatchRecordDO> equipmentTaskDispatchRecordDOS = new ArrayList<>();

    /**
     * EquipmentTaskLogDO
     */
    private List<EquipmentTaskLogDO> equipmentTaskLogDOS = new ArrayList<>();

    /**
     * 设备运维周期
     */
    private List<EquipmentOpsDO> equipmentOpsDOS = new ArrayList<>();

    /**
     * 查询设备任务
     */
    public Optional<EquipmentTaskDispatchRecordDO> queryEquipmentTask(EquipmentDO equipmentDO) {
        // 1.查询设备任务
        String equipmentNo = equipmentDO.getEquipmentNo();
        return equipmentTaskDispatchRecordDOS.stream()
                .filter(equipmentTaskDispatchRecordDO ->
                        DateUtil.format(equipmentTaskDispatchRecordDO.getTaskTime(), DatePattern.NORM_DATE_PATTERN)
                                .equals(DateUtil.format(DateUtil.date(), DatePattern.NORM_DATE_PATTERN))
                ).filter(equipmentTaskDispatchRecordDO ->
                        equipmentTaskDispatchRecordDO.getEquipmentNo().equals(equipmentNo)
                ).filter(equipmentTaskDispatchRecordDO -> equipmentTaskDispatchRecordDO.getTaskStatus().equals(1)
                ).max(Comparator.comparing(EquipmentTaskDispatchRecordDO::getTaskPriority));


    }


    /**
     * 定时分配任务
     * 每日23时前分配次日任务
     */
    public void dispatchEquipmentTask() {

        // TODO 1.集群环境获取乐观锁定 or 使用一致性任务中间件
        // 2.是否初次分配任务
        int maxTask = 4;
        if (equipmentTaskDispatchRecordDOS.isEmpty()) {
            List<EquipmentDO> equipmentCopyS = new ArrayList<>();
            List<EquipmentTaskDispatchRecordDO> equipmentTaskDispatchRecordTempS = new ArrayList<>();
            BeanUtil.copyProperties(equipmentDOS, equipmentCopyS);
            Map<String, List<TaskDO>> taskGroupByCity = taskDOS.stream()
                    .collect(Collectors.groupingBy(TaskDO::getTaskCityNo));
            // 执行结束后，taskGroupByCity只包含小于maxTask任务数量的city
            taskGroupByCity.forEach((cityNo, taskDOList) ->
                    fillExceedMaxTask(maxTask, equipmentCopyS, equipmentTaskDispatchRecordTempS, taskDOList));
            // 允许两个城市，可以直接做减法(不是最优解法，有优化空间)
            for (int i = 0; i < taskGroupByCity.size(); i++) {
                List<TaskDO> taskIList = taskGroupByCity.get(i);
                int searchSize = maxTask - taskIList.size();
                for (int j = 0; j < taskGroupByCity.size(); j++) {
                    List<TaskDO> taskJList = taskGroupByCity.get(j);
                    if(searchSize != taskJList.size()){
                        continue;
                    }
                    // 两个城市的任务数量和等于maxTask
                    EquipmentDO removedEquipmentDO = equipmentCopyS.remove(equipmentCopyS.size() - 1);
                    fillNonExceedMaxTask(taskIList.size(), 1,removedEquipmentDO, equipmentTaskDispatchRecordTempS, taskIList);
                    fillNonExceedMaxTask(taskJList.size(), 2,removedEquipmentDO, equipmentTaskDispatchRecordTempS, taskJList);
                    break;
                }
                // 不满足两个城市的任务数量和等于maxTask，也分配一个设备
                if(!taskIList.isEmpty()){
                    EquipmentDO removedEquipmentDO = equipmentCopyS.remove(equipmentCopyS.size() - 1);
                    fillNonExceedMaxTask(taskIList.size(), 1,removedEquipmentDO, equipmentTaskDispatchRecordTempS, taskIList);
                }
            }
            equipmentTaskDispatchRecordDOS.addAll(equipmentTaskDispatchRecordTempS);
        }else{
            // 3.非初次分配任务,
            // TODO copy 昨天的任务，然后修改任务时间
            // TODO 调整 优先级2的变成1 ，1 的变成2
            // TODO 替换需要运维的设备到新设备，或者替换成从运维中恢复回来的设备
            // TODO 新建任务同样的方式分配给新设备或者空闲设备
            //


        }



    }

    /**
     * fillExceedMaxTask
     *
     * @param maxTask                            maxTask
     * @param equipmentDOCopy                   equipmentDOCopy
     * @param equipmentTaskDispatchRecordDOTempS equipmentTaskDispatchRecordDOTempS
     * @param taskDOList                         taskDOList
     */
    private void fillNonExceedMaxTask(int maxTask,
                                   int taskPriority,
                                   EquipmentDO equipmentDOCopy,
                                   List<EquipmentTaskDispatchRecordDO> equipmentTaskDispatchRecordDOTempS,
                                   List<TaskDO> taskDOList) {
        if (taskDOList.size() >= maxTask) {
            for (int i = 0; i < maxTask; i++) {
                buildEquipmentTaskDispatchRecordDOTemp(equipmentTaskDispatchRecordDOTempS,
                        equipmentDOCopy,
                        taskDOList.remove(taskDOList.size() - 1),
                        taskPriority);
            }
        }

    }
    /**
     * fillExceedMaxTask
     *
     * @param maxTask                            maxTask
     * @param equipmentDOCopyS                   equipmentDOCopyS
     * @param equipmentTaskDispatchRecordDOTempS equipmentTaskDispatchRecordDOTempS
     * @param taskDOList                         taskDOList
     */
    private void fillExceedMaxTask(int maxTask,
                                   List<EquipmentDO> equipmentDOCopyS,
                                   List<EquipmentTaskDispatchRecordDO> equipmentTaskDispatchRecordDOTempS,
                                   List<TaskDO> taskDOList) {
        if (equipmentDOCopyS.isEmpty()) {
            return;
        }
        if (taskDOList.size() >= maxTask) {
            EquipmentDO removedEquipmentDO = equipmentDOCopyS.remove(equipmentDOCopyS.size() - 1);
            for (int i = 0; i < maxTask; i++) {
                buildEquipmentTaskDispatchRecordDOTemp(equipmentTaskDispatchRecordDOTempS,
                        removedEquipmentDO,
                        taskDOList.remove(taskDOList.size() - 1),
                        1);
            }
        }
        if (taskDOList.size() >= maxTask) {
            fillExceedMaxTask(maxTask, equipmentDOCopyS, equipmentTaskDispatchRecordDOTempS, taskDOList);
        }
    }

    /**
     * @param equipmentTaskDispatchRecordDOTempS equipmentTaskDispatchRecordDOTempS
     * @param removedEquipmentDO                 removedEquipmentDO
     * @param removeTaskDO                       removeTaskDO
     */
    private void buildEquipmentTaskDispatchRecordDOTemp(List<EquipmentTaskDispatchRecordDO> equipmentTaskDispatchRecordDOTempS,
                                                        EquipmentDO removedEquipmentDO,
                                                        TaskDO removeTaskDO,
                                                        int taskPriority
                                                        ) {
        equipmentTaskDispatchRecordDOTempS.add(EquipmentTaskDispatchRecordDO.builder()
                .taskNo(removeTaskDO.getTaskNo())
                .taskTime(DateUtil.offset(DateUtil.date(), DateField.DAY_OF_MONTH, 1))
                .taskStatus(1)
                .taskPriority(taskPriority)
                .equipmentNo(removedEquipmentDO.getEquipmentNo())
                .build());
    }

}

/**
 * 设备
 */
@Data
class EquipmentDO {
    /**
     * 设备编号
     */
    private String equipmentNo;
    /**
     * 设备状态 0-休息 1-使用中
     */
    private Integer equipmentStatus;
    /**
     * 设备类型
     */
    private Integer equipmentType;

}

/**
 * 任务
 */
@Data
class TaskDO {
    /**
     * 任务编号
     */
    private String taskNo;
    /**
     * 任务状态 0-已废弃 1-可执行
     */
    private Integer taskStatus;
    /**
     * 任务城市
     */
    private String taskCityNo;
    /**
     * 任务内容
     */
    private String taskContent;

}

/**
 * 设备任务调度
 */
@Data
@Builder
class EquipmentTaskDispatchRecordDO {
    /**
     * 设备编号
     */
    private String equipmentNo;
    /**
     * 任务编号
     */
    private String taskNo;
    /**
     * 任务城市
     */
    private String taskCityNo;
    /**
     * 任务执行日期
     */
    private Date taskTime;
    /**
     * 任务执行状态 0-已致性 1-未执行
     */
    private Integer taskStatus;
    /**
     * 任务优先级
     */
    private Integer taskPriority;

}

/**
 * 设备任务调度
 */
@Data
class EquipmentTaskStatisticsDO {
    /**
     * 设备编号
     */
    private String equipmentNo;
    /**
     * 任务城市
     */
    private String taskCityNo;
    /**
     * 任务执行日期
     */
    private Date taskTime;
    /**
     * 执行数量
     */
    private Integer taskCount;
}

/**
 * 设备任务日志
 */
@Data
class EquipmentTaskLogDO {
    /**
     * 设备编号
     */
    private String equipmentNo;
    /**
     * 任务编号
     */
    private String taskNo;
    /**
     * 任务城市
     */
    private String taskCityNo;
    /**
     * 任务执行日期
     */
    private Date taskTime;

}

/**
 * 设备运维周期
 */
@Data
class EquipmentOpsDO {
    /**
     * 设备编号
     */
    private String equipmentNo;
    /**
     * 运维类型 0-日常运维 1-每周休息 2-每月休息
     */
    private Integer equipmentOpsType;
    /**
     * 上次运维时间
     */
    private String lastOpsTime;

}
