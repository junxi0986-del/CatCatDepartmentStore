// Cloudflare Pages Function：将 /api/* 反向代理到后端服务
// 部署时在 Pages 项目「设置 -> 环境变量」中配置 BACKEND_URL（例如 https://xxx.onrender.com）
// WebSocket（/api/ws/chat）升级请求会原样透传，由 Cloudflare 运行时桥接
export async function onRequest({ request, env }) {
  const backend = (env.BACKEND_URL || '').replace(/\/+$/, '')
  if (!backend) {
    return new Response('未配置 BACKEND_URL 环境变量，请在 Pages 项目设置中添加', { status: 500 })
  }
  const url = new URL(request.url)
  let rest = url.pathname.slice('/api'.length) || '/'
  // 兼容数据库中以 /api 开头的资源路径，避免出现 /api/api/...
  if (rest.startsWith('/api/')) rest = rest.slice('/api'.length)
  const upstream = `${backend}/api${rest}${url.search}`
  return fetch(new Request(upstream, request))
}
