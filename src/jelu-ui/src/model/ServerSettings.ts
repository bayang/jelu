import { PluginInfo } from "./PluginInfo";

export interface ServerSettings {
  metadataFetchEnabled: boolean,
  metadataFetchCalibreEnabled: boolean,
  appVersion: string,
  ldapEnabled: boolean,
  metadataPlugins: Array<PluginInfo>
}
