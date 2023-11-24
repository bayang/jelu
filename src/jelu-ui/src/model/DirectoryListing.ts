export interface DirectoryListing {
    parent?: string,
    directories: Array<Path>,
}
export interface Path {
    type: string,
    name: string,
    path: string
}
export interface DirectoryRequest {
    path: string,
    reason: string,
}
