from rest_framework.viewsets import ModelViewSet
from rest_framework.permissions import IsAuthenticated
from rest_framework.exceptions import ValidationError

from .models import Favorite
from .serializers import FavoriteSerializer


class FavoriteViewSet(ModelViewSet):

    serializer_class = FavoriteSerializer
    permission_classes = [IsAuthenticated]

    def get_queryset(self):
        return Favorite.objects.filter(user=self.request.user)

    def perform_create(self, serializer):

        tour = serializer.validated_data.get("tour")
        user = self.request.user

        if Favorite.objects.filter(user=user, tour=tour).exists():
            raise ValidationError("Already in favorites")

        serializer.save(user=user)