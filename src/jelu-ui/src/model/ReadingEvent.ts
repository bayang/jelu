export default interface ReadingEvent {
    id?: string,
    creationDate?: string,
    // name: string,
    eventType: ReadingEventType,
  }

export enum ReadingEventType {
    FINISHED = 'FINISHED',
    DROPPED = 'DROPPED',
    CURRENTLY_READING = 'CURRENTLY_READING',
    NONE = 'NONE'
}

