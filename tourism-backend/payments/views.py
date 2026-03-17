from rest_framework.viewsets import ModelViewSet
from rest_framework.permissions import IsAuthenticated
from .models import Payment
from .serializers import PaymentSerializer
from rest_framework.decorators import action
from rest_framework.response import Response
from rest_framework import status

class PaymentViewSet(ModelViewSet):
    serializer_class = PaymentSerializer
    permission_classes = [IsAuthenticated]

    def get_queryset(self):
       return Payment.objects.filter(booking__user=self.request.user)

    def perform_create(self, serializer):
        booking = serializer.validated_data["booking"]
        if booking.user != self.request.user:
            raise serializers.ValidationError("You cannot pay for someone else's booking")
        serializer.save(status="pending")
    
    @action(detail=True, methods=["POST"])
    def pay(self, request, pk=None):
        payment = self.get_object()
        if payment.status != "pending":
            return Response({"error": "Payment already processed"}, status=status.HTTP_400_BAD_REQUEST)
        payment.status = "paid"
        payment.save()
        return Response({"status": payment.status})