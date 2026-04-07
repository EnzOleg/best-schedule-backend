from rest_framework import serializers
from .models import Favorite
from tours.models import Tour
from tours.serializers import TourSerializer


class FavoriteSerializer(serializers.ModelSerializer):

    tour = serializers.PrimaryKeyRelatedField(
        queryset=Tour.objects.all()
    )

    tour_data = TourSerializer(source="tour", read_only=True)

    class Meta:
        model = Favorite
        fields = ["id", "tour", "tour_data", "created_at"]
        read_only_fields = ["created_at"]