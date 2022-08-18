export interface ImportConfigurationDto{
    shouldFetchMetadata: boolean,
    shouldFetchCovers: boolean,
    importSource: ImportSource
}

export enum ImportSource {
    GOODREADS = 'GOODREADS',
    STORYGRAPH = 'STORYGRAPH',
    LIBRARYTHING = 'LIBRARYTHING',
    ISBN_LIST = 'ISBN_LIST'
}
