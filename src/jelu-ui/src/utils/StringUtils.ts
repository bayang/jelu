import { ReadingEventType } from "../model/ReadingEvent";

export class StringUtils {

    public static isNotBlank(param: string|null): boolean {
        if (param !== undefined && param !== null && param.trim().length > 0) {
            return true;
        }
        return false;
    }

    public static readingEventTypeForValue(val: string): ReadingEventType {
        return ReadingEventType[val as keyof typeof ReadingEventType];
    }
}
