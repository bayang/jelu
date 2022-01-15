import { ReadingEventType } from "../model/ReadingEvent";

export class StringUtils {

    public static isNotBlank(param: string|null|undefined): boolean {
        return !this.isBlank(param)
    }

    public static isBlank(param: string|null|undefined): boolean {
        if (param !== undefined && param !== null && param.trim().length > 0) {
            return false;
        }
        return true;
    }

    public static readingEventTypeForValue(val: string): ReadingEventType {
        return ReadingEventType[val as keyof typeof ReadingEventType];
    }
}
