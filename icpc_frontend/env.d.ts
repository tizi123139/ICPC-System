/// <reference types="vite/client" />

interface ImportMetaEnv {
  /** 后端接口基础路径，默认 /api/v1 */
  readonly VITE_API_BASE_URL: string
  /** 是否使用本地 Mock 数据（true / false），后端联调时置为 false */
  readonly VITE_USE_MOCK: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
