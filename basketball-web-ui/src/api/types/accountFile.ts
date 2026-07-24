export interface FileUploadDto {
  fileId: string
  accessUrl: string
  usageType: string
  originalName?: string
  fileSize?: number
}
