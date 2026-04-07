from rest_framework.generics import CreateAPIView
from .models import User
from .serializers import RegisterSerializer, UserSerializer
from rest_framework.authtoken.models import Token
from rest_framework.decorators import api_view, permission_classes
from rest_framework.response import Response
from rest_framework.permissions import IsAuthenticated
from django.contrib.auth import authenticate


class RegisterView(CreateAPIView):
    queryset = User.objects.all()
    serializer_class = RegisterSerializer


@api_view(["POST"])
def login_view(request):

    username = request.data.get("username")
    password = request.data.get("password")

    if not username or not password:
        return Response({"error": "Username and password required"}, status=400)

    user = authenticate(username=username, password=password)

    if user is None:
        return Response({"error": "Invalid credentials"}, status=400)

    token, created = Token.objects.get_or_create(user=user)

    return Response({"token": token.key})


@api_view(["GET", "PATCH"])
@permission_classes([IsAuthenticated])
def me(request):

    user = request.user

    if request.method == "GET":
        serializer = UserSerializer(user)
        return Response(serializer.data)


    if request.method == "PATCH":

        if not request.data:
            return Response({"error": "No data provided"}, status=400)

        serializer = UserSerializer(user, data=request.data, partial=True)

        if serializer.is_valid():
            serializer.save()
            return Response(serializer.data)

        return Response(serializer.errors, status=400)