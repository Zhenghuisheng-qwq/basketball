import request from '@/api/request'
import type { ApiResult } from '@/api/types/common'
import type { NotificationSettingDto, PrivacySettingDto } from '@/api/types/accountSetting'

export function getPrivacySetting() {
  return request.get<any, ApiResult<PrivacySettingDto>>('/basket/account/setting/privacy')
}

export function updatePrivacySetting(payload: PrivacySettingDto) {
  return request.put<any, ApiResult<PrivacySettingDto>>('/basket/account/setting/privacy', payload)
}

export function getNotificationSetting() {
  return request.get<any, ApiResult<NotificationSettingDto>>('/basket/account/setting/notification')
}

export function updateNotificationSetting(payload: NotificationSettingDto) {
  return request.put<any, ApiResult<NotificationSettingDto>>('/basket/account/setting/notification', payload)
}
