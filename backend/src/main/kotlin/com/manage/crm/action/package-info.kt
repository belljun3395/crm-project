package com.manage.crm.action

import org.springframework.modulith.ApplicationModule

@ApplicationModule(
    type = ApplicationModule.Type.OPEN,
    allowedDependencies = [
        "support",
        "infrastructure",
        "email",
        "event",
        "user",
        "webhook",
    ],
)
class Metadata
