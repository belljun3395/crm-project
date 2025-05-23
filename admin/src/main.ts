import { NestFactory } from '@nestjs/core';

import { AppModule } from './app.module.js';

async function bootstrap() {
  async function bootstrap() {
    const app = await NestFactory.create(AppModule);
    const port = process.env.PORT ?? 3000;
    await app.listen(port);
    console.log(`Application is running on: ${await app.getUrl()}`);
  }
  bootstrap();
}
bootstrap();
