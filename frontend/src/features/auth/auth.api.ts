import { request } from '@/shared/api/http'

export const authApi = {
  login: (username: string) =>
    request<{ accessToken: string; expiresIn: number }>('/api/auth/token', {
      method: 'POST',
      body: JSON.stringify({ username }),
    }),
}
