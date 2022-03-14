import dayjs from "dayjs";

export default function useDates() {

    function formatDateString(dateString: string | null | undefined): string {
        if (dateString != null) {
          const date = dayjs(dateString)
          return date.format('D MMMM YYYY')
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

