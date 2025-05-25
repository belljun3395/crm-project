import { 
  BrowseUsersUseCaseOut, 
  EnrollUserUseCaseOut, 
  GetTotalUserCountUseCaseOut 
} from './User';
import { 
  SearchEventsUseCaseOut, 
  PostEventUseCaseOut, 
  PostCampaignUseCaseOut 
} from './Event';
import { 
  BrowseTemplateUseCaseOut, 
  PostTemplateUseCaseOut, 
  SendNotificationEmailUseCaseOut, 
  PostEmailNotificationSchedulesUseCaseOut, 
  BrowseEmailNotificationSchedulesUseCaseOut, 
  DeleteTemplateUseCaseOut, 
  CancelNotificationEmailUseCaseOut 
} from './Email';

export interface SuccessBodyBrowseUsersUseCaseOut {
  data: BrowseUsersUseCaseOut;
  message: string;
}

export interface SuccessBodyEnrollUserUseCaseOut {
  data: EnrollUserUseCaseOut;
  message: string;
}

export interface SuccessBodyGetTotalUserCountUseCaseOut {
  data: GetTotalUserCountUseCaseOut;
  message: string;
}

export interface SuccessBodySearchEventsUseCaseOut {
  data: SearchEventsUseCaseOut;
  message: string;
}

export interface SuccessBodyPostEventUseCaseOut {
  data: PostEventUseCaseOut;
  message: string;
}

export interface SuccessBodyPostCampaignUseCaseOut {
  data: PostCampaignUseCaseOut;
  message: string;
}

export interface SuccessBodyBrowseTemplateUseCaseOut {
  data: BrowseTemplateUseCaseOut;
  message: string;
}

export interface SuccessBodyPostTemplateUseCaseOut {
  data: PostTemplateUseCaseOut;
  message: string;
}

export interface SuccessBodySendNotificationEmailUseCaseOut {
  data: SendNotificationEmailUseCaseOut;
  message: string;
}

export interface SuccessBodyPostEmailNotificationSchedulesUseCaseOut {
  data: PostEmailNotificationSchedulesUseCaseOut;
  message: string;
}

export interface SuccessBodyBrowseEmailNotificationSchedulesUseCaseOut {
  data: BrowseEmailNotificationSchedulesUseCaseOut;
  message: string;
}

export interface SuccessBodyDeleteTemplateUseCaseOut {
  data: DeleteTemplateUseCaseOut;
  message: string;
}

export interface SuccessBodyCancelNotificationEmailUseCaseOut {
  data: CancelNotificationEmailUseCaseOut;
  message: string;
}
