package com.manage.crm.webhook

import org.springframework.modulith.ApplicationModule

@ApplicationModule(
    type = ApplicationModule.Type.OPEN,
    allowedDependencies = [
        "support",
        "infrastructure",
        "user",
        "email",
        "event"
    ]
)
class Metadata
