import { DateUtils } from "../utils/DateUtils"
import dayjs from "dayjs";

export default function useDates() {

    function formatDateString(dateString: string | Date | null | undefined): string {
        if (dateString != null) {
          return DateUtils.formatDate(dateString)
        }
        return ''
    }

    function formatDate(date: Date | null | undefined): string {
        if (date != null) {
          return dayjs(date).format('D MMMM YYYY')
        }
        return ''
    }

    return {
        formatDate,
        formatDateString
    }
}

