<script setup lang="ts">
import { useOruga } from "@oruga-ui/oruga-next"
import { useTitle } from '@vueuse/core'
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useStore } from 'vuex'
import { key } from '../store'
import dataService from "../services/DataService"
import { ObjectUtils } from "../utils/ObjectUtils"
import { ApiToken, TokenScope, CreateApiToken } from "../model/ApiToken"
import dayjs from "dayjs"

const { t } = useI18n({
  inheritLocale: true,
  useScope: 'global'
})

useTitle('Jelu | API Tokens')

const store = useStore(key)
const oruga = useOruga()

const tokens = ref<ApiToken[]>([])
const scopes = ref<TokenScope[]>([])
const loading = ref(false)
const showCreateModal = ref(false)
const showTokenModal = ref(false)
const createdRawToken = ref('')

const form = ref<CreateApiToken>({
  name: '',
  scopes: [],
  expiresAt: undefined
})

const expirationOption = ref('never')
const customExpiration = ref('')

onMounted(async () => {
  await loadTokens()
  await loadScopes()
})

async function loadTokens() {
  loading.value = true
  try {
    tokens.value = await dataService.getApiTokens()
  } catch (err: any) {
    console.log('error loading tokens', err)
    ObjectUtils.toast(oruga, "danger", t('api_tokens.load_error'), 4000)
  } finally {
    loading.value = false
  }
}

async function loadScopes() {
  try {
    scopes.value = await dataService.getApiTokenScopes()
  } catch (err: any) {
    console.log('error loading scopes', err)
  }
}

async function createToken() {
  try {
    // Handle expiration
    let expiresAt: string | undefined = undefined
    if (expirationOption.value === '30days') {
      expiresAt = dayjs().add(30, 'day').toISOString()
    } else if (expirationOption.value === '90days') {
      expiresAt = dayjs().add(90, 'day').toISOString()
    } else if (expirationOption.value === '1year') {
      expiresAt = dayjs().add(1, 'year').toISOString()
    } else if (expirationOption.value === 'custom' && customExpiration.value) {
      expiresAt = dayjs(customExpiration.value).toISOString()
    }

    const result = await dataService.createApiToken({
      name: form.value.name,
      scopes: form.value.scopes,
      expiresAt: expiresAt
    })
    
    // Show the raw token
    createdRawToken.value = result.rawToken
    showCreateModal.value = false
    showTokenModal.value = true
    
    // Reset form
    form.value = { name: '', scopes: [], expiresAt: undefined }
    expirationOption.value = 'never'
    customExpiration.value = ''
    
    // Reload tokens list
    await loadTokens()
    
    ObjectUtils.toast(oruga, "success", t('api_tokens.created'), 4000)
  } catch (err: any) {
    console.log('error creating token', err)
    ObjectUtils.toast(oruga, "danger", t('api_tokens.create_error'), 4000)
  }
}

async function revokeToken(token: ApiToken) {
  if (!confirm(t('api_tokens.revoke_confirm', { name: token.name }))) {
    return
  }
  
  try {
    await dataService.deleteApiToken(token.id)
    await loadTokens()
    ObjectUtils.toast(oruga, "success", t('api_tokens.revoked'), 4000)
  } catch (err: any) {
    console.log('error revoking token', err)
    ObjectUtils.toast(oruga, "danger", t('api_tokens.revoke_error'), 4000)
  }
}

async function toggleTokenActive(token: ApiToken) {
  try {
    await dataService.updateApiToken(token.id, { isActive: !token.isActive })
    await loadTokens()
    ObjectUtils.toast(oruga, "success", token.isActive ? t('api_tokens.deactivated') : t('api_tokens.activated'), 4000)
  } catch (err: any) {
    console.log('error updating token', err)
    ObjectUtils.toast(oruga, "danger", t('api_tokens.update_error'), 4000)
  }
}

function copyToken() {
  navigator.clipboard.writeText(createdRawToken.value)
  ObjectUtils.toast(oruga, "success", t('api_tokens.copied'), 2000)
}

function closeTokenModal() {
  showTokenModal.value = false
  createdRawToken.value = ''
}

function formatDate(dateStr: string | undefined) {
  if (!dateStr) return '-'
  return dayjs(dateStr).format('YYYY-MM-DD HH:mm')
}

function formatScopes(scopeList: string[]) {
  return scopeList.join(', ')
}

const isFormValid = computed(() => {
  return form.value.name.length >= 1 && form.value.scopes.length >= 1
})

const scopesByCategory = computed(() => {
  const grouped: Record<string, TokenScope[]> = {}
  for (const scope of scopes.value) {
    if (!grouped[scope.category]) {
      grouped[scope.category] = []
    }
    grouped[scope.category].push(scope)
  }
  return grouped
})

function toggleScope(scopeName: string) {
  const idx = form.value.scopes.indexOf(scopeName)
  if (idx >= 0) {
    form.value.scopes.splice(idx, 1)
  } else {
    form.value.scopes.push(scopeName)
  }
}

</script>

<template>
  <div class="grid grid-cols-1 justify-center justify-items-center justify-self-center">
    <h1 class="typewriter text-2xl mb-3 capitalize">
      {{ t('api_tokens.title') }}
    </h1>
    
    <p class="text-base-content/70 mb-4 max-w-2xl text-center">
      {{ t('api_tokens.description') }}
    </p>

    <button
      class="btn btn-primary mb-6"
      @click="showCreateModal = true"
    >
      <i class="bx bx-plus mr-2" />
      {{ t('api_tokens.create') }}
    </button>

    <!-- Tokens list -->
    <div class="w-full max-w-4xl">
      <div v-if="loading" class="flex justify-center">
        <span class="loading loading-spinner loading-lg" />
      </div>
      
      <div v-else-if="tokens.length === 0" class="text-center text-base-content/60">
        {{ t('api_tokens.no_tokens') }}
      </div>
      
      <div v-else class="space-y-4">
        <div
          v-for="token in tokens"
          :key="token.id"
          class="card bg-base-200 shadow-md"
        >
          <div class="card-body">
            <div class="flex justify-between items-start">
              <div>
                <h3 class="card-title">
                  {{ token.name }}
                  <span
                    v-if="!token.isActive"
                    class="badge badge-warning ml-2"
                  >
                    {{ t('api_tokens.inactive') }}
                  </span>
                  <span
                    v-if="token.expiresAt && dayjs(token.expiresAt).isBefore(dayjs())"
                    class="badge badge-error ml-2"
                  >
                    {{ t('api_tokens.expired') }}
                  </span>
                </h3>
                <p class="text-sm text-base-content/70">
                  {{ t('api_tokens.scopes') }}: {{ formatScopes(token.scopes) }}
                </p>
              </div>
              <div class="flex gap-2">
                <button
                  class="btn btn-sm btn-ghost"
                  :title="token.isActive ? t('api_tokens.deactivate') : t('api_tokens.activate')"
                  @click="toggleTokenActive(token)"
                >
                  <i :class="token.isActive ? 'bx bx-pause' : 'bx bx-play'" />
                </button>
                <button
                  class="btn btn-sm btn-ghost btn-error"
                  :title="t('api_tokens.revoke')"
                  @click="revokeToken(token)"
                >
                  <i class="bx bx-trash" />
                </button>
              </div>
            </div>
            
            <div class="flex flex-wrap gap-4 text-sm text-base-content/60 mt-2">
              <span>
                <i class="bx bx-calendar mr-1" />
                {{ t('api_tokens.created_at') }}: {{ formatDate(token.createdAt) }}
              </span>
              <span>
                <i class="bx bx-time mr-1" />
                {{ t('api_tokens.last_used') }}: {{ formatDate(token.lastUsedAt) }}
              </span>
              <span>
                <i class="bx bx-bar-chart mr-1" />
                {{ t('api_tokens.usage_count') }}: {{ token.usageCount }}
              </span>
              <span v-if="token.expiresAt">
                <i class="bx bx-calendar-x mr-1" />
                {{ t('api_tokens.expires_at') }}: {{ formatDate(token.expiresAt) }}
              </span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Create Token Modal -->
    <div v-if="showCreateModal" class="modal modal-open">
      <div class="modal-box max-w-2xl">
        <h3 class="font-bold text-lg mb-4">
          {{ t('api_tokens.create_new') }}
        </h3>
        
        <div class="form-control mb-4">
          <label class="label">
            <span class="label-text">{{ t('api_tokens.token_name') }}</span>
          </label>
          <input
            v-model="form.name"
            type="text"
            class="input input-bordered"
            :placeholder="t('api_tokens.name_placeholder')"
          >
        </div>

        <div class="form-control mb-4">
          <label class="label">
            <span class="label-text">{{ t('api_tokens.select_scopes') }}</span>
          </label>
          <div class="space-y-4">
            <div v-for="(categoryScopes, category) in scopesByCategory" :key="category">
              <h4 class="font-semibold text-sm mb-2">{{ category }}</h4>
              <div class="flex flex-wrap gap-2">
                <label
                  v-for="scope in categoryScopes"
                  :key="scope.name"
                  class="cursor-pointer"
                >
                  <input
                    type="checkbox"
                    class="checkbox checkbox-sm mr-1"
                    :checked="form.scopes.includes(scope.name)"
                    @change="toggleScope(scope.name)"
                  >
                  <span class="text-sm" :title="scope.description">
                    {{ scope.name }}
                  </span>
                </label>
              </div>
            </div>
          </div>
        </div>

        <div class="form-control mb-4">
          <label class="label">
            <span class="label-text">{{ t('api_tokens.expiration') }}</span>
          </label>
          <select v-model="expirationOption" class="select select-bordered">
            <option value="never">{{ t('api_tokens.never_expires') }}</option>
            <option value="30days">{{ t('api_tokens.expires_30days') }}</option>
            <option value="90days">{{ t('api_tokens.expires_90days') }}</option>
            <option value="1year">{{ t('api_tokens.expires_1year') }}</option>
            <option value="custom">{{ t('api_tokens.expires_custom') }}</option>
          </select>
          <input
            v-if="expirationOption === 'custom'"
            v-model="customExpiration"
            type="date"
            class="input input-bordered mt-2"
          >
        </div>

        <div class="modal-action">
          <button class="btn" @click="showCreateModal = false">
            {{ t('labels.cancel') }}
          </button>
          <button
            class="btn btn-primary"
            :disabled="!isFormValid"
            @click="createToken"
          >
            {{ t('api_tokens.create') }}
          </button>
        </div>
      </div>
      <div class="modal-backdrop" @click="showCreateModal = false" />
    </div>

    <!-- Token Created Modal (shows raw token once) -->
    <div v-if="showTokenModal" class="modal modal-open">
      <div class="modal-box">
        <h3 class="font-bold text-lg mb-4">
          {{ t('api_tokens.token_created') }}
        </h3>
        
        <div class="alert alert-warning mb-4">
          <i class="bx bx-error-circle" />
          <span>{{ t('api_tokens.copy_warning') }}</span>
        </div>

        <div class="form-control">
          <label class="label">
            <span class="label-text">{{ t('api_tokens.your_token') }}</span>
          </label>
          <div class="flex gap-2">
            <input
              :value="createdRawToken"
              type="text"
              class="input input-bordered flex-1 font-mono text-sm"
              readonly
            >
            <button class="btn btn-primary" @click="copyToken">
              <i class="bx bx-copy" />
            </button>
          </div>
        </div>

        <div class="modal-action">
          <button class="btn btn-primary" @click="closeTokenModal">
            {{ t('api_tokens.done') }}
          </button>
        </div>
      </div>
      <div class="modal-backdrop" @click="closeTokenModal" />
    </div>
  </div>
</template>

<style lang="scss" scoped>
</style>
