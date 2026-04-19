from pydantic import BaseModel


class LoginRequest(BaseModel):
    username: str
    password: str


class TokenResponse(BaseModel):
    access_token: str
    token_type: str = "bearer"
    username: str


class UserRead(BaseModel):
    id: int
    username: str
    display_name: str
    role: str = "member"
    instance_id: str = ""
    edition: str = "personal"
    default_space_slug: str = ""
