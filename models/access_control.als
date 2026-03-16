sig User {
    roles: set Role,
    owns: set Resource
}

sig Role {
    permissions: set Permission
}

sig Permission {}

sig Resource {
    requiredPermission: one Permission
}

sig Session {
    user: one User,
    activeRoles: set Role
}

fact ActiveRolesSubsetOfUserRoles {
    all s: Session |
        s.activeRoles in s.user.roles
}

fact CanAccessOwned {
    all u: User | all r: Resource |
        r in u.owns implies
        r.requiredPermission in u.roles.permissions
}

fact NoEmptySession {
    all s: Session | some s.activeRoles
}

pred someAccessControl {}

run someAccessControl for
    exactly 4 User,
    exactly 3 Role,
    exactly 4 Permission,
    exactly 5 Resource,
    exactly 3 Session