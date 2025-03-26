package com.manage.crm.email

import org.springframework.modulith.ApplicationModule

@ApplicationModule(
    type = ApplicationModule.Type.OPEN,
    allowedDependencies = [
        "support",
        "infrastructure",
        "user"
    ]
)
class Metadata
