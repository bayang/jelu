import { Author } from "./Author";
import { SeriesOrder } from "./Series";
import { Tag } from "./Tag";

export interface Wrapper {
    label: string,
    value: Author|Tag|SeriesOrder
}
