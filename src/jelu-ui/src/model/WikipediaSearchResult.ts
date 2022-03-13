

export interface WikipediaSearchResultElement {
    id: number,
    key: string,
    title: string,
    excerpt: string,
    description?: string,
    matchedTitle?: string,
    thumbnail?: Thumbnail
}

export interface Thumbnail {
    mimetype: string,
    size?: string,
    width: number,
    height: number,
    duration: number,
    url: string
}

export interface WikipediaSearchResult {
    pages: Array<WikipediaSearchResultElement>
}
