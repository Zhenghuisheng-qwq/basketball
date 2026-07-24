export interface MessageItemDto {
  receiverId: string
  messageType?: string
  title: string
  content?: string
  bizType?: string
  bizId?: string
  readStatus?: number
  sentTime?: string
  readTime?: string
  redirectType?: string
  redirectValue?: string
}
