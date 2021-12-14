import dayjs from "dayjs";

export class DateUtils {

    public static formatDate(dateString: any) {
        const date = dayjs(dateString)
        return date.format('D MMMM YYYY')
    }
}
