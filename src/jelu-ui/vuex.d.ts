import { RouteLocationNormalized } from 'vue-router';
import { Store } from 'vuex';
import { ServerSettings } from './src/model/ServerSettings';
import { User } from './src/model/User';

declare module '@vue/runtime-core' {
  // declare your own store states
interface State {
    isLogged: boolean,
    isInitialSetup : boolean,
    user : User| null,
    serverSettings: ServerSettings,
    route: RouteLocationNormalized | null
  }

  // provide typings for `this.$store`
interface ComponentCustomProperties {
    $store: Store<State>
  }
}
