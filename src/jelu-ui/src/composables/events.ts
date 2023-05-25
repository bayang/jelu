import { useI18n } from 'vue-i18n';
import { ReadingEventType } from '../model/ReadingEvent';


export default function useEvents() {
  const { t } = useI18n({
    inheritLocale: true,
    useScope: 'global'
  })

  const eventClass = (type: ReadingEventType) => {
    if (type === ReadingEventType.FINISHED) {
      return "badge-info";
    } else if (type === ReadingEventType.DROPPED) {
      return "badge-error";
    } else if (
      type === ReadingEventType.CURRENTLY_READING
    ) {
      return "badge-success";
    } else return "";
};

const eventLabel = (type: ReadingEventType) => {
    if (type === ReadingEventType.FINISHED) {
      return t('reading_events.finished');
    } else if (type === ReadingEventType.DROPPED) {
      return t('reading_events.dropped');
    } else if (type === ReadingEventType.CURRENTLY_READING) {
      return t('reading_events.reading');
    } else return "";
};

    return {
        eventClass,
        eventLabel
    }
}
