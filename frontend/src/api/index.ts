import request from '@/utils/request'

// ---------------- 类型 ----------------

export interface UserProfile {
  id: number
  username: string
  realName: string
  userType: number
  dept?: string
  roles: string[]
  perms: string[]
}

export interface LoginResult {
  token: string
  expiresIn: number
  user: UserProfile
}

export interface MenuNode {
  id: number
  title: string
  path?: string
  perm?: string
  icon?: string
  children: MenuNode[]
}

export interface PageResult<T> {
  total: number
  records: T[]
}

export interface Course {
  id?: number
  code: string
  name: string
  credit: number
  hours?: number
  category?: string
  dept?: string
  description?: string
}

export interface Offering {
  id?: number
  termId?: number
  termName?: string
  courseId?: number
  courseCode?: string
  courseName?: string
  credit?: number
  category?: string
  teacherId?: number
  teacherName?: string
  clazzName?: string
  weekday: number
  startSection: number
  endSection: number
  location?: string
  capacity: number
  enrolled?: number
  status?: number
  timeText?: string
  selected?: boolean
  conflict?: boolean
  conflictWith?: string
}

export interface Term {
  id: number
  name: string
  startDate?: string
  endDate?: string
  isCurrent?: number
}

export interface Clazz {
  id: number
  name: string
  grade?: number
  major?: string
}

export interface MyCourse {
  enrollmentId: number
  offeringId: number
  courseCode: string
  courseName: string
  credit: number
  category?: string
  teacherName?: string
  weekday: number
  startSection: number
  endSection: number
  location?: string
  timeText: string
  status: number
  total?: number
  gpa?: number
}

export interface ScoreRow {
  id: number
  enrollmentId: number
  studentNo: string
  studentName: string
  courseCode: string
  courseName: string
  credit: number
  termName: string
  usualScore?: number
  examScore?: number
  total?: number
  gpa?: number
}

export interface ScoreSummary {
  gpa: number
  credits: number
  courseCount: number
}

export interface RosterRow {
  enrollmentId: number
  studentId: number
  studentNo: string
  studentName: string
  clazzName?: string
  usualScore?: number
  examScore?: number
  total?: number
  gpa?: number
}

export interface SysUserRow {
  id: number
  username: string
  realName: string
  userType: number
  dept?: string
  phone?: string
  email?: string
  status: number
  roles: string[]
  createdAt?: string
}

export interface RoleRow {
  id: number
  code: string
  name: string
  remark?: string
  menuIds: number[]
}

export interface MenuRow {
  id: number
  parentId: number
  title: string
  path?: string
  perm?: string
  menuType: number
  icon?: string
  sortNo: number
}

export interface NoticeRow {
  id: number
  title: string
  content?: string
  publisherId?: number
  publisherName?: string
  publishedAt?: string
}

export interface Dashboard {
  courseCount: number
  offeringCount: number
  studentCount: number
  teacherCount: number
  enrollmentCount: number
  avgCapacityUsage: number
  currentTermName: string
  weekLoad: { weekday: number; label: string; count: number }[]
  hotCourses: { courseName: string; teacherName: string; enrolled: number; capacity: number }[]
  notices: NoticeRow[]
  myCourseCount?: number
  myCredits?: number
  myGpa?: number
  myOfferingCount?: number
  myStudentCount?: number
}

// ---------------- 认证 ----------------

export const authApi = {
  login: (data: { username: string; password: string }) =>
    request.post<unknown, LoginResult>('/auth/login', data),
  profile: () => request.get<unknown, UserProfile>('/auth/profile'),
  menus: () => request.get<unknown, MenuNode[]>('/auth/menus'),
  logout: () => request.post('/auth/logout'),
}

// ---------------- 工作台 ----------------

export const dashboardApi = {
  load: () => request.get<unknown, Dashboard>('/dashboard'),
}

// ---------------- 课程 ----------------

export const courseApi = {
  page: (params: { keyword?: string; category?: string; page?: number; size?: number }) =>
    request.get<unknown, PageResult<Course>>('/courses', { params }),
  all: () => request.get<unknown, Course[]>('/courses/all'),
  create: (data: Course) => request.post<unknown, number>('/courses', data),
  update: (id: number, data: Course) => request.put(`/courses/${id}`, data),
  remove: (id: number) => request.delete(`/courses/${id}`),
}

// ---------------- 开课 ----------------

export const offeringApi = {
  list: (params: { termId?: number; keyword?: string }) =>
    request.get<unknown, Offering[]>('/offerings', { params }),
  create: (data: Offering) => request.post<unknown, number>('/offerings', data),
  update: (id: number, data: Offering) => request.put(`/offerings/${id}`, data),
  remove: (id: number) => request.delete(`/offerings/${id}`),
  terms: () => request.get<unknown, Term[]>('/terms'),
  currentTerm: () => request.get<unknown, Term>('/terms/current'),
  clazzes: () => request.get<unknown, Clazz[]>('/clazzes'),
  teachers: () => request.get<unknown, { id: number; realName: string }[]>('/teachers'),
}

// ---------------- 选课 ----------------

export const enrollmentApi = {
  my: (params: { termId?: number; onlyActive?: boolean }) =>
    request.get<unknown, MyCourse[]>('/enrollments/my', { params }),
  select: (offeringId: number) => request.post<unknown, number>('/enrollments', { offeringId }),
  drop: (id: number) => request.delete(`/enrollments/${id}`),
}

// ---------------- 成绩 ----------------

export const scoreApi = {
  list: (params: { termId?: number; keyword?: string }) =>
    request.get<unknown, ScoreRow[]>('/scores', { params }),
  summary: () => request.get<unknown, ScoreSummary>('/scores/summary'),
  roster: (offeringId: number) =>
    request.get<unknown, RosterRow[]>('/scores/roster', { params: { offeringId } }),
  save: (data: { enrollmentId: number; usualScore: number; examScore: number }) =>
    request.post('/scores', data),
}

// ---------------- 用户 / 角色 ----------------

export const userApi = {
  page: (params: { keyword?: string; userType?: number; page?: number; size?: number }) =>
    request.get<unknown, PageResult<SysUserRow>>('/users', { params }),
  create: (data: Record<string, unknown>) => request.post<unknown, number>('/users', data),
  update: (id: number, data: Record<string, unknown>) => request.put(`/users/${id}`, data),
  remove: (id: number) => request.delete(`/users/${id}`),
}

export const roleApi = {
  list: () => request.get<unknown, RoleRow[]>('/roles'),
  menus: () => request.get<unknown, MenuRow[]>('/roles/menus'),
  assign: (id: number, menuIds: number[]) => request.put(`/roles/${id}/menus`, menuIds),
}

// ---------------- 公告 ----------------

export const noticeApi = {
  list: (keyword?: string) => request.get<unknown, NoticeRow[]>('/notices', { params: { keyword } }),
  publish: (data: { title: string; content?: string }) => request.post<unknown, number>('/notices', data),
  update: (id: number, data: { title: string; content?: string }) =>
    request.put(`/notices/${id}`, data),
  remove: (id: number) => request.delete(`/notices/${id}`),
}
