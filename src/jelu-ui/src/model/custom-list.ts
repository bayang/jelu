export interface CustomList {
    id?: string,
    name: string,
    tags: string,
    public: boolean,
    actionable: boolean,
    creationDate?: string,
    modificationDate?: string,
}

export interface CustomListRemoveDto{
    books: Array<string>,
    tags: Array<string>,
}
