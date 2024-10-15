package com.easypan.component;

import com.easypan.mapper.FileInfoMapper;
import com.easypan.mapper.UserInfoMapper;
import com.easypan.pojo.UserInfo;
import com.easypan.pojo.constants.Constants;
import com.easypan.pojo.dto.DownloadFileDto;
import com.easypan.pojo.dto.SysSettingsDto;
import com.easypan.pojo.dto.UserSpaceDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class RedisComponent {
    @Autowired
    private RedisUtils redisUtils;
    @Autowired
    private FileInfoMapper fileInfoMapper;
    @Autowired
    private UserInfoMapper userInfoMapper;

    //保存邮箱验证码
    public SysSettingsDto getSysSettingDto() {
        SysSettingsDto sysSettingsDto = (SysSettingsDto) redisUtils.get(Constants.REDIS_KEY_SYS_SETTING);
        if (sysSettingsDto == null) {
            sysSettingsDto = new SysSettingsDto();
            redisUtils.set(Constants.REDIS_KEY_SYS_SETTING, sysSettingsDto);
        }
        return sysSettingsDto;
    }
    //保存系统设置
    public void saveSysSettingDto(SysSettingsDto sysSettingsDto) {
        redisUtils.set(Constants.REDIS_KEY_SYS_SETTING, sysSettingsDto);
    }
    //保存空间
    public void saveUserSpaceUse(String userId, UserSpaceDto userSpaceDto) {
        redisUtils.setex(Constants.REDIS_KEY_USER_SPACE_USE + userId, userSpaceDto, Constants.REDIS_KEY_EXPIRES_DAY);
    }
    //重置用户空间
    public UserSpaceDto resetUserSpaceUse(String userId) {
        UserSpaceDto userSpaceDto = new UserSpaceDto();
        Long userSpace=fileInfoMapper.selectSum(userId);
        userSpaceDto.setUseSpace(userSpace);
        UserInfo userInfo = userInfoMapper.selectById(userId);
        userSpaceDto.setTotalSpace(userInfo.getTotalSpace());
        redisUtils.setex(Constants.REDIS_KEY_USER_SPACE_USE+userId,userSpaceDto,Constants.REDIS_KEY_EXPIRES_DAY);
        return userSpaceDto;
    }
    public UserSpaceDto getUserSpaceUse(String userId) {
        UserSpaceDto userSpaceDto = (UserSpaceDto) redisUtils.get(Constants.REDIS_KEY_USER_SPACE_USE + userId);
        if (userSpaceDto == null) {
            userSpaceDto = new UserSpaceDto();
            //查询文件后根据文件设置
            Long userSpace=fileInfoMapper.selectSum(userId);
            userSpaceDto.setUseSpace(userSpace);
            userSpaceDto.setTotalSpace(getSysSettingDto().getUserInitUseSpace() * Constants.MB);
            saveUserSpaceUse(userId, userSpaceDto);
        }
        return userSpaceDto;
    }
    //保存临时文件大小
    public void saveFileTempSize(String userId,String fileId,Long fileSize) {
        Long currentSize=getFileTempSize(userId,fileId);//获取之前文件的大小
        //更新临时文件大小
        redisUtils.setex(Constants.REDIS_KEY_USER_FILE_TEMP_SIZE+userId+fileId,currentSize+fileSize,Constants.REDIS_KEY_EXPIRES_TWO_HOUR);
    }
    //获取临时文件大小
    public Long getFileTempSize(String userId, String fileId) {
        Long currentSize=getFileSizeFromRedis(Constants.REDIS_KEY_USER_FILE_TEMP_SIZE+userId+fileId);
        return currentSize;
    }
    //从redis获取文件大小
    private Long getFileSizeFromRedis(String key) {
        Object obj = redisUtils.get(key);
        if(obj == null) {
            return 0L;
        }
        if(obj instanceof Long) {
            return (Long) obj;
        }
        if(obj instanceof Integer) {
            return ((Integer)obj).longValue();
        }
        return 0L;
    }
    //保存code
    public void saveDownloadCode(String code, DownloadFileDto fileDto) {
        redisUtils.setex(Constants.REDIS_KEY_DOWNLOAD+code,fileDto,Constants.REDIS_KEY_EXPIRES_FIVE_MIN);
    }
    //获取code
    public DownloadFileDto getDownloadCode(String code) {
        return (DownloadFileDto) redisUtils.get(Constants.REDIS_KEY_DOWNLOAD+code);
    }
}
