import { MaybeRef } from '@vueuse/shared/'
import type { Ref } from '@vueuse/router/node_modules/vue-demi'
import { computed, nextTick, unref } from '@vueuse/router/node_modules/vue-demi'
import { useRoute, useRouter } from 'vue-router'

export function useRouteQueryArray(name: string): Ref<null | string | string[]>
export function useRouteQueryArray<T extends null | undefined | string | string[] = null | string | string[]>(name: string, defaultValue?: T, options?: ReactiveRouteOptions): Ref<T>
export function useRouteQueryArray<T extends string | string[]>(
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
      if (data == null)
        return defaultValue ?? null
      if (Array.isArray(data))
        return data.filter(Boolean)
      return [data]
    },
    set(v) {
      nextTick(() => {
        router[unref(mode)]({ ...route, query: { ...route.query, [name]: v === defaultValue || v === null ? undefined : v } })
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
