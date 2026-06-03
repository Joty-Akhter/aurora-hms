import axios from 'axios';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '';

const crmApi = axios.create({
  baseURL: `${API_BASE_URL}/api/crm`,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Lead interfaces
export interface Lead {
  leadId?: string;
  organizationId: string;
  leadNumber?: string;
  firstName?: string;
  lastName?: string;
  email?: string;
  phone?: string;
  mobile?: string;
  company?: string;
  jobTitle?: string;
  leadSourceId?: string;
  sourceCampaign?: string;
  status?: string;
  rating?: string;
  leadScore?: number;
  street?: string;
  city?: string;
  state?: string;
  postalCode?: string;
  country?: string;
  createdAt?: string;
  industry?: string;
  companySize?: string;
  annualRevenue?: number;
  website?: string;
  ownerId?: string;
  notes?: string;
  tags?: string[];
}

export interface Account {
  accountId?: string;
  organizationId: string;
  accountNumber?: string;
  accountName: string;
  accountType?: string;
  industry?: string;
  phone?: string;
  fax?: string;
  email?: string;
  website?: string;
  billingStreet?: string;
  billingCity?: string;
  billingState?: string;
  billingPostalCode?: string;
  billingCountry?: string;
  shippingStreet?: string;
  shippingCity?: string;
  shippingState?: string;
  shippingPostalCode?: string;
  shippingCountry?: string;
  ownerId?: string;
  isActive?: boolean;
  description?: string;
  notes?: string;
}

export interface Contact {
  contactId?: string;
  organizationId: string;
  accountId?: string;
  firstName?: string;
  lastName?: string;
  salutation?: string;
  jobTitle?: string;
  department?: string;
  email?: string;
  phone?: string;
  mobile?: string;
  isPrimary?: boolean;
  isActive?: boolean;
  notes?: string;
}

export interface LeadSource {
  sourceId?: string;
  organizationId: string;
  sourceName: string;
  sourceCode: string;
  sourceType?: string;
  isActive?: boolean;
  description?: string;
}

// Lead APIs
export const getLeads = (organizationId: string, params?: {
  status?: string;
  ownerId?: string;
  search?: string;
}) => {
  return crmApi.get<Lead[]>('/leads', { params: { organizationId, ...params } });
};

export const getLeadById = (id: string) => {
  return crmApi.get<Lead>(`/leads/${id}`);
};

export const createLead = (lead: Lead) => {
  return crmApi.post<Lead>('/leads', lead);
};

export const updateLead = (id: string, lead: Partial<Lead>) => {
  return crmApi.put<Lead>(`/leads/${id}`, lead);
};

export const deleteLead = (id: string) => {
  return crmApi.delete(`/leads/${id}`);
};

export const assignLead = (id: string, ownerId: string) => {
  return crmApi.post(`/leads/${id}/assign`, { ownerId });
};

export const qualifyLead = (id: string, qualifiedBy: string) => {
  return crmApi.post(`/leads/${id}/qualify`, { qualifiedBy });
};

export const disqualifyLead = (id: string, reason: string) => {
  return crmApi.post(`/leads/${id}/disqualify`, { reason });
};

export const convertLead = (id: string, convertedBy: string, accountId?: string, contactId?: string, opportunityId?: string) => {
  return crmApi.post(`/leads/${id}/convert`, { convertedBy, accountId, contactId, opportunityId });
};

export const getLeadActivities = (leadId: string) => {
  return crmApi.get(`/leads/${leadId}/activities`);
};

export const createLeadActivity = (leadId: string, activity: any) => {
  return crmApi.post(`/leads/${leadId}/activities`, activity);
};

export const getLeadDashboardStats = (organizationId: string) => {
  return crmApi.get('/leads/dashboard/stats', { params: { organizationId } });
};

// Account APIs
export const getAccounts = (organizationId: string, params?: {
  accountType?: string;
  search?: string;
}) => {
  return crmApi.get<Account[]>('/accounts', { params: { organizationId, ...params } });
};

export const getAccountById = (id: string) => {
  return crmApi.get<Account>(`/accounts/${id}`);
};

export const createAccount = (account: Account) => {
  return crmApi.post<Account>('/accounts', account);
};

export const updateAccount = (id: string, account: Partial<Account>) => {
  return crmApi.put<Account>(`/accounts/${id}`, account);
};

export const deleteAccount = (id: string) => {
  return crmApi.delete(`/accounts/${id}`);
};

// Contact APIs
export const getContacts = (organizationId: string, params?: {
  accountId?: string;
  search?: string;
}) => {
  return crmApi.get<Contact[]>('/contacts', { params: { organizationId, ...params } });
};

export const getContactById = (id: string) => {
  return crmApi.get<Contact>(`/contacts/${id}`);
};

export const createContact = (contact: Contact) => {
  return crmApi.post<Contact>('/contacts', contact);
};

export const updateContact = (id: string, contact: Partial<Contact>) => {
  return crmApi.put<Contact>(`/contacts/${id}`, contact);
};

export const deleteContact = (id: string) => {
  return crmApi.delete(`/contacts/${id}`);
};

// Lead Source APIs
export const getLeadSources = (organizationId: string) => {
  return crmApi.get<LeadSource[]>('/lead-sources', { params: { organizationId } });
};

// Opportunity interfaces
export interface OpportunityStage {
  stageId?: string;
  organizationId: string;
  stageName: string;
  stageCode: string;
  stageOrder: number;
  probability: number;
  isClosedWon?: boolean;
  isClosedLost?: boolean;
  isActive?: boolean;
  description?: string;
}

export interface Opportunity {
  opportunityId?: string;
  organizationId: string;
  opportunityNumber?: string;
  opportunityName: string;
  accountId?: string;
  contactId?: string;
  leadId?: string;
  stageId?: string;
  type?: string;
  amount: number;
  currency?: string;
  probability?: number;
  expectedRevenue?: number;
  closeDate?: string;
  expectedCloseDate?: string;
  createdDate?: string;
  ownerId?: string;
  status?: string;
  leadSourceId?: string;
  sourceCampaign?: string;
  description?: string;
  nextStep?: string;
  competitors?: string;
  lossReason?: string;
  lossDescription?: string;
  winDescription?: string;
  daysInStage?: number;
  totalDaysOpen?: number;
  tags?: string[];
  priority?: string;
}

export interface OpportunityProduct {
  oppProductId?: string;
  opportunityId: string;
  organizationId: string;
  productCode?: string;
  productName: string;
  productDescription?: string;
  quantity: number;
  unitPrice: number;
  discountPercent?: number;
  discountAmount?: number;
  taxPercent?: number;
  subtotal?: number;
  discountTotal?: number;
  taxAmount?: number;
  lineTotal?: number;
  notes?: string;
  lineOrder?: number;
}

export interface OpportunityActivity {
  activityId?: string;
  opportunityId: string;
  organizationId: string;
  activityType: string;
  subject: string;
  description?: string;
  activityDate?: string;
  durationMinutes?: number;
  status?: string;
  priority?: string;
  assignedTo?: string;
  completedBy?: string;
  outcome?: string;
  nextAction?: string;
}

// Opportunity APIs
export const getOpportunities = async (organizationId: string, status?: string) => {
  const response = await crmApi.get('/opportunities', { 
    params: { organizationId, status } 
  });
  return response.data;
};

export const getOpportunityById = async (id: string) => {
  const response = await crmApi.get(`/opportunities/${id}`);
  return response.data;
};

export const getOpportunityByNumber = async (organizationId: string, number: string) => {
  const response = await crmApi.get(`/opportunities/number/${number}`, {
    params: { organizationId }
  });
  return response.data;
};

export const createOpportunity = async (opportunity: Opportunity) => {
  const response = await crmApi.post('/opportunities', opportunity);
  return response.data;
};

export const updateOpportunity = async (id: string, opportunity: Partial<Opportunity>) => {
  const response = await crmApi.put(`/opportunities/${id}`, opportunity);
  return response.data;
};

export const deleteOpportunity = async (id: string) => {
  return crmApi.delete(`/opportunities/${id}`);
};

export const getClosingSoon = async (organizationId: string, days: number = 30) => {
  const response = await crmApi.get('/opportunities/closing-soon', {
    params: { organizationId, days }
  });
  return response.data;
};

export const moveOpportunityToStage = async (id: string, stageId: string) => {
  const response = await crmApi.post(`/opportunities/${id}/move-stage`, { stageId });
  return response.data;
};

export const markOpportunityWon = async (id: string, winDescription: string) => {
  const response = await crmApi.post(`/opportunities/${id}/won`, { winDescription });
  return response.data;
};

export const markOpportunityLost = async (id: string, lossReason: string, lossDescription: string) => {
  const response = await crmApi.post(`/opportunities/${id}/lost`, { lossReason, lossDescription });
  return response.data;
};

// Opportunity Products APIs
export const getOpportunityProducts = async (opportunityId: string) => {
  const response = await crmApi.get(`/opportunities/${opportunityId}/products`);
  return response.data;
};

export const addOpportunityProduct = async (opportunityId: string, product: OpportunityProduct) => {
  const response = await crmApi.post(`/opportunities/${opportunityId}/products`, product);
  return response.data;
};

export const updateOpportunityProduct = async (productId: string, product: Partial<OpportunityProduct>) => {
  const response = await crmApi.put(`/opportunities/products/${productId}`, product);
  return response.data;
};

export const deleteOpportunityProduct = async (productId: string) => {
  return crmApi.delete(`/opportunities/products/${productId}`);
};

// Opportunity Activities APIs
export const getOpportunityActivities = async (opportunityId: string) => {
  const response = await crmApi.get(`/opportunities/${opportunityId}/activities`);
  return response.data;
};

export const addOpportunityActivity = async (opportunityId: string, activity: OpportunityActivity) => {
  const response = await crmApi.post(`/opportunities/${opportunityId}/activities`, activity);
  return response.data;
};

export const updateOpportunityActivity = async (activityId: string, activity: Partial<OpportunityActivity>) => {
  const response = await crmApi.put(`/opportunities/activities/${activityId}`, activity);
  return response.data;
};

export const deleteOpportunityActivity = async (activityId: string) => {
  return crmApi.delete(`/opportunities/activities/${activityId}`);
};

// Pipeline APIs
export const getOpportunityStages = async (organizationId: string, activeOnly: boolean = false) => {
  const response = await crmApi.get('/pipeline/stages', {
    params: { organizationId, activeOnly }
  });
  return response.data;
};

export const getOpportunityStageById = async (id: string) => {
  const response = await crmApi.get(`/pipeline/stages/${id}`);
  return response.data;
};

export const getOpportunityStageByCode = async (organizationId: string, code: string) => {
  const response = await crmApi.get(`/pipeline/stages/code/${code}`, {
    params: { organizationId }
  });
  return response.data;
};

export const createOpportunityStage = async (stage: OpportunityStage) => {
  const response = await crmApi.post('/pipeline/stages', stage);
  return response.data;
};

export const updateOpportunityStage = async (id: string, stage: Partial<OpportunityStage>) => {
  const response = await crmApi.put(`/pipeline/stages/${id}`, stage);
  return response.data;
};

export const deleteOpportunityStage = async (id: string) => {
  return crmApi.delete(`/pipeline/stages/${id}`);
};

export const getPipelineStats = async (organizationId: string) => {
  const response = await crmApi.get('/pipeline/stats', {
    params: { organizationId }
  });
  return response.data;
};

// Campaign APIs
export const getCampaigns = async (organizationId: string, status?: string, type?: string, search?: string) => {
  const response = await crmApi.get('/campaigns', {
    params: { organizationId, status, type, search }
  });
  return response.data;
};

export const getCampaignById = async (id: string) => {
  const response = await crmApi.get(`/campaigns/${id}`);
  return response.data;
};

export const createCampaign = async (campaign: any) => {
  const response = await crmApi.post('/campaigns', campaign);
  return response.data;
};

export const updateCampaign = async (id: string, campaign: any) => {
  const response = await crmApi.put(`/campaigns/${id}`, campaign);
  return response.data;
};

export const deleteCampaign = async (id: string) => {
  return crmApi.delete(`/campaigns/${id}`);
};

export const getCampaignStats = async (id: string) => {
  const response = await crmApi.get(`/campaigns/${id}/stats`);
  return response.data;
};

export const getCampaignMembers = async (campaignId: string) => {
  const response = await crmApi.get(`/campaigns/${campaignId}/members`);
  return response.data;
};

export const addCampaignMember = async (campaignId: string, member: any) => {
  const response = await crmApi.post(`/campaigns/${campaignId}/members`, member);
  return response.data;
};

// Task APIs
export const getTasks = async (organizationId: string, assignedTo?: string, status?: string, priority?: string) => {
  const response = await crmApi.get('/tasks', {
    params: { organizationId, assignedTo, status, priority }
  });
  return response.data;
};

export const getTaskById = async (id: string) => {
  const response = await crmApi.get(`/tasks/${id}`);
  return response.data;
};

export const createTask = async (task: any) => {
  const response = await crmApi.post('/tasks', task);
  return response.data;
};

export const updateTask = async (id: string, task: any) => {
  const response = await crmApi.put(`/tasks/${id}`, task);
  return response.data;
};

export const deleteTask = async (id: string) => {
  return crmApi.delete(`/tasks/${id}`);
};

export const getTasksDueToday = async (organizationId: string, userId: string) => {
  const response = await crmApi.get('/tasks/due-today', {
    params: { organizationId, userId }
  });
  return response.data;
};

export const getOverdueTasks = async (organizationId: string, userId: string) => {
  const response = await crmApi.get('/tasks/overdue', {
    params: { organizationId, userId }
  });
  return response.data;
};

export const completeTask = async (id: string, completedBy: string, notes: string) => {
  const response = await crmApi.post(`/tasks/${id}/complete`, { completedBy, notes });
  return response.data;
};

export const getTaskStats = async (organizationId: string, userId: string) => {
  const response = await crmApi.get('/tasks/stats', {
    params: { organizationId, userId }
  });
  return response.data;
};

// Event APIs
export const getEvents = async (organizationId: string, organizerId?: string, status?: string, startDate?: string, endDate?: string) => {
  const response = await crmApi.get('/events', {
    params: { organizationId, organizerId, status, startDate, endDate }
  });
  return response.data;
};

export const getEventById = async (id: string) => {
  const response = await crmApi.get(`/events/${id}`);
  return response.data;
};

export const createEvent = async (event: any) => {
  const response = await crmApi.post('/events', event);
  return response.data;
};

export const updateEvent = async (id: string, event: any) => {
  const response = await crmApi.put(`/events/${id}`, event);
  return response.data;
};

export const deleteEvent = async (id: string) => {
  return crmApi.delete(`/events/${id}`);
};

export const completeEvent = async (id: string, outcome: string, notes: string) => {
  const response = await crmApi.post(`/events/${id}/complete`, { outcome, notes });
  return response.data;
};

// Email Template APIs
export const getEmailTemplates = async (organizationId: string, activeOnly?: boolean, templateType?: string) => {
  const response = await crmApi.get('/email-templates', {
    params: { organizationId, activeOnly, templateType }
  });
  return response.data;
};

export const getEmailTemplateById = async (id: string) => {
  const response = await crmApi.get(`/email-templates/${id}`);
  return response.data;
};

export const createEmailTemplate = async (template: any) => {
  const response = await crmApi.post('/email-templates', template);
  return response.data;
};

export const updateEmailTemplate = async (id: string, template: any) => {
  const response = await crmApi.put(`/email-templates/${id}`, template);
  return response.data;
};

export const deleteEmailTemplate = async (id: string) => {
  return crmApi.delete(`/email-templates/${id}`);
};

// Case APIs
export const getCases = async (organizationId: string, status?: string, priority?: string, assignedTo?: string, search?: string) => {
  const response = await crmApi.get('/cases', {
    params: { organizationId, status, priority, assignedTo, search }
  });
  return response.data;
};

export const getCaseById = async (id: string) => {
  const response = await crmApi.get(`/cases/${id}`);
  return response.data;
};

export const createCase = async (caseData: any) => {
  const response = await crmApi.post('/cases', caseData);
  return response.data;
};

export const updateCase = async (id: string, caseData: any) => {
  const response = await crmApi.put(`/cases/${id}`, caseData);
  return response.data;
};

export const deleteCase = async (id: string) => {
  return crmApi.delete(`/cases/${id}`);
};

export const assignCase = async (id: string, assignedTo: string) => {
  const response = await crmApi.post(`/cases/${id}/assign`, { assignedTo });
  return response.data;
};

export const resolveCase = async (id: string, resolvedBy: string, resolution: string) => {
  const response = await crmApi.post(`/cases/${id}/resolve`, { resolvedBy, resolution });
  return response.data;
};

export const closeCase = async (id: string) => {
  const response = await crmApi.post(`/cases/${id}/close`);
  return response.data;
};

export const rateCase = async (id: string, rating: number, feedback: string) => {
  const response = await crmApi.post(`/cases/${id}/rate`, { rating, feedback });
  return response.data;
};

export const getCaseStats = async (organizationId: string) => {
  const response = await crmApi.get('/cases/stats', {
    params: { organizationId }
  });
  return response.data;
};

export const getCaseComments = async (caseId: string) => {
  const response = await crmApi.get(`/cases/${caseId}/comments`);
  return response.data;
};

export const addCaseComment = async (caseId: string, comment: any) => {
  const response = await crmApi.post(`/cases/${caseId}/comments`, comment);
  return response.data;
};

// Knowledge Base APIs
export const getKnowledgeBaseArticles = async (organizationId: string, publicOnly?: boolean, featuredOnly?: boolean, category?: string, search?: string) => {
  const response = await crmApi.get('/knowledge-base', {
    params: { organizationId, publicOnly, featuredOnly, category, search }
  });
  return response.data;
};

export const getKnowledgeBaseArticleById = async (id: string) => {
  const response = await crmApi.get(`/knowledge-base/${id}`);
  return response.data;
};

export const createKnowledgeBaseArticle = async (article: any) => {
  const response = await crmApi.post('/knowledge-base', article);
  return response.data;
};

export const updateKnowledgeBaseArticle = async (id: string, article: any) => {
  const response = await crmApi.put(`/knowledge-base/${id}`, article);
  return response.data;
};

export const deleteKnowledgeBaseArticle = async (id: string) => {
  return crmApi.delete(`/knowledge-base/${id}`);
};

export const publishKnowledgeBaseArticle = async (id: string, publishedBy: string) => {
  const response = await crmApi.post(`/knowledge-base/${id}/publish`, { publishedBy });
  return response.data;
};

export const markArticleHelpful = async (id: string) => {
  return crmApi.post(`/knowledge-base/${id}/helpful`);
};

export const markArticleNotHelpful = async (id: string) => {
  return crmApi.post(`/knowledge-base/${id}/not-helpful`);
};

// CRM Analytics APIs
export const getCrmAnalyticsDashboard = async (organizationId: string) => {
  const response = await crmApi.get('/analytics/dashboard', {
    params: { organizationId }
  });
  return response.data;
};

export const getLeadAnalytics = async (organizationId: string) => {
  const response = await crmApi.get('/analytics/leads', {
    params: { organizationId }
  });
  return response.data;
};

export const getOpportunityAnalytics = async (organizationId: string) => {
  const response = await crmApi.get('/analytics/opportunities', {
    params: { organizationId }
  });
  return response.data;
};

export const getSupportAnalytics = async (organizationId: string) => {
  const response = await crmApi.get('/analytics/support', {
    params: { organizationId }
  });
  return response.data;
};

export default crmApi;


