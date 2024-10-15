package com.easypan.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.easypan.enums.FileDelFlagEnums;
import com.easypan.pojo.FileInfo;
import com.easypan.service.FileInfoService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class FileCleanTask {
    @Resource
    private FileInfoService fileInfoService;

    @Scheduled(fixedDelay = 1000 * 60 * 3)
    public void execute() {
        LambdaQueryWrapper<FileInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(FileInfo::getDelFlag, FileDelFlagEnums.RECYCLE.getFlag());
        queryWrapper.leSql(FileInfo::getRecoveryTime, "date_sub(now(),interval 10 day)");
        List<FileInfo> fileInfoList = fileInfoService.list(queryWrapper);
        Map<String, List<FileInfo>> fileInfoMap = fileInfoList.stream().collect(Collectors.groupingBy(FileInfo::getUserId));
        for (Map.Entry<String, List<FileInfo>> entry : fileInfoMap.entrySet()) {
            List<String> fileIds = entry.getValue().stream().map(p -> p.getFileId()).collect(Collectors.toList());
            fileInfoService.delFile(entry.getKey(), String.join(",", fileIds), false);
        }
    }
}
/*


这段代码的主要功能是定期检查文件的状态，并根据回收站和恢复时间的条件，删除特定用户的文件。让我们逐句详细解释这段代码的含义。

1. @Scheduled(fixedDelay = 1000*60*3)
解释: 这是一个 Spring 框架中的定时任务注解，表示该方法每隔固定时间执行一次。fixedDelay = 1000*60*3 表示在上次执行完成后 3 分钟（1000 毫秒 * 60 秒 * 3 分钟）再次执行该方法。
2. public void execute()
解释: 这是定时任务执行的主要方法体。当定时器触发时，将调用这个方法。
3. LambdaQueryWrapper<FileInfo> queryWrapper = new LambdaQueryWrapper<>();
解释: 初始化一个 LambdaQueryWrapper 对象，用于构造数据库查询条件。LambdaQueryWrapper 提供了一种类型安全的方式来创建查询条件，避免手写 SQL。
4. queryWrapper.eq(FileInfo::getDelFlag, FileDelFlagEnums.RECYCLE.getFlag());
解释: 添加查询条件，表示查询 FileInfo 表中 delFlag 字段等于 FileDelFlagEnums.RECYCLE.getFlag() 的记录。即只查询状态为“回收站”的文件。
5. queryWrapper.leSql(FileInfo::getRecoveryTime, "date_sub(now(),interval 10 day)");
解释: 添加查询条件，查询文件的恢复时间 recoveryTime 小于或等于当前时间的 10 天前。这里使用了 SQL 的 date_sub 函数，表示当前时间减去 10 天。
6. List<FileInfo> fileInfoList = fileInfoService.list(queryWrapper);
解释: 根据前面构造的 queryWrapper 查询符合条件的 FileInfo 记录，并将结果存储在 fileInfoList 中。
7. Map<String, List<FileInfo>> fileInfoMap = fileInfoList.stream().collect(Collectors.groupingBy(FileInfo::getUserId));
解释: 通过 Stream API 将查询到的文件按 userId 分组。groupingBy 将 fileInfoList 按 userId 组织成一个 Map，键是 userId，值是该用户下的 FileInfo 列表。
8. for (Map.Entry<String, List<FileInfo>> entry : fileInfoMap.entrySet())
解释: 遍历 fileInfoMap 的每个条目（Entry），entry.getKey() 是用户 ID，entry.getValue() 是该用户的文件列表。
9. List<String> fileIds = entry.getValue().stream().map(p -> p.getFileId()).collect(Collectors.toList());
解释: 对于当前用户的文件列表，使用 Stream API 提取出每个文件的 fileId，并将其收集到一个 List 中。
10. fileInfoService.delFile(entry.getKey(), String.join(",", fileIds), false);
解释: 调用 fileInfoService 的 delFile 方法，执行删除操作。
entry.getKey() 是用户 ID。
String.join(",", fileIds) 将文件 ID 列表转换为以逗号分隔的字符串。
false 可能是一个标志，表示删除操作的某个特定行为（具体行为需要根据 delFile 方法的实现来理解）。
总结
这段代码的主要作用是：
每隔 3 分钟，查询 FileInfo 表中处于“回收站”状态、且恢复时间已经超过 10 天的文件，将这些文件按 userId 分组，并对每个用户执行文件删除操作。







 */