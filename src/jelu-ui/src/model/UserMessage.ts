export interface UserMessage {
    id?: string,
    creationDate?: string,
    modificationDate?: Date,
    category: MessageCategory,
    message?: string,
    link?: string,
    read?: boolean
  }

  export interface UpdateUserMessage {
    category?: MessageCategory,
    message?: string,
    link?: string,
    read?: boolean
  }

  export enum MessageCategory {
    SUCCESS = 'SUCCESS',
    INFO = 'INFO',
    WARNING = 'WARNING',
    ERROR = 'ERROR'
  }