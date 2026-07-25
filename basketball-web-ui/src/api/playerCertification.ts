import request from '@/api/request'
import type { ApiResult } from '@/api/types/common'
import type {
  PlayerCertCurrentDto,
  PlayerCertDraftPayload,
  PlayerCertMaterialBindPayload,
  PlayerCertMaterialDto,
  PlayerCertSubmitPayload,
} from '@/api/types/playerCertification'

export function getPlayerCertificationCurrent() {
  return request.get<any, ApiResult<PlayerCertCurrentDto>>('/basket/player/certification/current')
}

export function savePlayerCertificationDraft(payload: PlayerCertDraftPayload) {
  return request.post<any, ApiResult<PlayerCertCurrentDto>>('/basket/player/certification/draft', payload)
}

export function bindPlayerCertificationMaterial(payload: PlayerCertMaterialBindPayload) {
  return request.post<any, ApiResult<PlayerCertMaterialDto>>('/basket/player/certification/material', payload)
}

export function deletePlayerCertificationMaterial(materialId: string) {
  return request.delete<any, ApiResult<null>>(`/basket/player/certification/material/${materialId}`)
}

export function submitPlayerCertification(payload: PlayerCertSubmitPayload) {
  return request.post<any, ApiResult<PlayerCertCurrentDto>>('/basket/player/certification/submit', payload)
}

export function withdrawPlayerCertification() {
  return request.post<any, ApiResult<PlayerCertCurrentDto>>('/basket/player/certification/withdraw')
}
