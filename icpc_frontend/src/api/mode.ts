/**
 * Mock 开关：仅当显式设置 VITE_USE_MOCK=true 时启用。
 * 开发/生产联调默认关闭，前端统一请求后端真实数据库数据。
 */
export const USE_MOCK = import.meta.env.VITE_USE_MOCK === 'true'
