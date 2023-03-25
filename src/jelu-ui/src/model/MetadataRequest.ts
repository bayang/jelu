import { PluginInfo } from "./PluginInfo";

export interface MetadataRequest {
    title?: string,
    isbn?:string,
    authors?: string,
    plugins?: Array<PluginInfo>,
}
