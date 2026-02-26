package com.manage.crm.journey

import org.springframework.modulith.ApplicationModule

@ApplicationModule(
    type = ApplicationModule.Type.OPEN,
    allowedDependencies = [
        "support",
        "infrastructure",
        "event",
        "action",
        "user",
        "segment"
    ]
)
class Metadata
