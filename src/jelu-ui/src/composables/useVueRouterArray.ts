import { MaybeRef } from '@vueuse/router/node_modules/@vueuse/shared'
import { computed, nextTick, unref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
// import {ReactiveRouteOptions} from '@vueuse/router'

export function useRouteQueryArray<T extends string[]>(
    name: string,
    defaultValue?: T,
    {
      mode = 'replace',
      route = useRoute(),
      router = useRouter(),
    }: ReactiveRouteOptions = {},
  ) {
    return computed<any>({
      get() {
        const data = route.query[name]
        console.log("qu a data " + data)
        if (data == null)
          return defaultValue ?? null
        if (Array.isArray(data))
          return data.filter(Boolean)
        return [data]
      },
      set(v) {
        nextTick(() => {
          router[unref(mode)]({ query: { ...route.query, [name]: v } })
        })
      },
    })
  }

  export interface ReactiveRouteOptions {
    /**
     * Mode to update the router query, ref is also acceptable
     *
     * @default 'replace'
     */
    mode?: MaybeRef<'replace' | 'push'>
  
    /**
     * Route instance, use `useRoute()` if not given
     */
    route?: ReturnType<typeof useRoute>
  
    /**
     * Router instance, use `useRouter()` if not given
     */
    router?: ReturnType<typeof useRouter>
  }