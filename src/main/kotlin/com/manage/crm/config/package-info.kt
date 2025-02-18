package com.manage.crm.config

import org.springframework.modulith.ApplicationModule

@ApplicationModule(
    type = ApplicationModule.Type.OPEN,
    allowedDependencies = [
        "email",
        "user"
    ]
)
class Metadata
