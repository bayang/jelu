
export interface WikipediaPageResult {
    type: string,
    title: string,
    displayTitle: string,
    wikibaseItem: string,
    pageId: number,
    lang: string,
    description: string,
    extract: string,
    extractHtml: string,
    contentUrls: ContentUrlList,
    thumbnail: PageThumbnail,
    originalImage: PageThumbnail
}

export interface PageThumbnail {
    source: string,
    width: number,
    height: number
}

export interface ContentUrlList {
    desktop: ContentUrl,
    mobile: ContentUrl
}

export interface ContentUrl {
    page: string,
    revisions: string,
    edit: string,
    talk: string
}
