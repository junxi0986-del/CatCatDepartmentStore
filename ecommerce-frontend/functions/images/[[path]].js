// Cloudflare Pages Function：将 /images/* 反向代理到后端 /api/images/*
export async function onRequest({ request, env }) {
  const backend = (env.BACKEND_URL || '').replace(/\/+$/, '')
  if (!backend) {
    return new Response('未配置 BACKEND_URL 环境变量，请在 Pages 项目设置中添加', { status: 500 })
  }
  const url = new URL(request.url)
  const rest = url.pathname.slice('/images'.length) || ''
  const upstream = `${backend}/api/images${rest}${url.search}`
  return fetch(new Request(upstream, request))
}
