import { Store } from 'vuex'
import { ServerSettings } from './src/model/ServerSettings';
import { User } from './src/model/User';

declare module '@vue/runtime-core' {
  // declare your own store states
interface State {
    count: number,
    isLogged: boolean,
    isInitialSetup : boolean,
    user : User| null,
    entryPoint: string,
    serverSettings: ServerSettings
  }

  // provide typings for `this.$store`
interface ComponentCustomProperties {
    $store: Store<State>
  }
}
