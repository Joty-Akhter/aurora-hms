const appName: string = import.meta.env.VITE_APP_NAME || 'Aurora HMS';
const appYear: number = new Date().getFullYear();

const appConfig = {
  appName,
  appYear,
} as const;

export default appConfig;
